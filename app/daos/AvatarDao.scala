package daos

import java.io.File
import java.sql.Blob
import java.util.UUID

import daos.schema.UploadTableDef
import models.UploadedImage
import models.database.UploadDB
import play.api.{Logger, Play}
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio.DBIOAction
import slick.driver.JdbcProfile
import slick.jdbc.GetResult
import slick.driver.MySQLDriver.api._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait AvatarDao {
  def has(email: String, width: Int, height: Int): Future[Boolean]
  def getAll(email: String) : Future[List[UploadedImage]]
  def getSelected(email: String, width: Int, height: Int): Future[Option[UploadedImage]]
  def get(uuid: UUID, email: String): Future[Option[UploadedImage]]
  def getThumb(uuid: UUID, width: Int, height: Int, email: String): Future[Option[UploadedImage]]

  def select(uuid: UUID, email: String): Future[Boolean]
  def add(image: File, fileName: String, contentType: Option[String], email: String, selected: Boolean): Future[Option[UploadedImage]]
  def replaceThumbs(uuid: UUID, thumbs: List[UploadedImage], email: String): Future[Either[Exception, List[UploadedImage]]]
  def updateEmail(previousEmail: String, newEmail: String): Future[Int]
  def remove(uuid: UUID, email: String) : Future[Int]
}

class MariadbAvatarDao extends AvatarDao {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  val uploads = TableQuery[UploadTableDef]

  val logger = Logger(this.getClass)

  implicit val getUploadResult =
    GetResult(r => UploadDB(
      r.nextLong, UUID.fromString(r.nextString), r.nextStringOption().map(UUID.fromString( _ )), r.nextString,
      r.nextString, r.nextInt, r.nextInt, r.nextBlob(), r.nextString(), r.nextBoolean()))

  private def mapper(result : Seq[(Long, UUID, Option[UUID], String, String, Int, Int, Blob, String, Boolean)]): Future[List[UploadedImage]] =
    Future.sequence(result.map(t => {
      val thumbs = dbConfig.db.run(uploads.filter(_.parentId === t._2).result).flatMap(mapper( _ ))
      thumbs.map(UploadDB(t).toUploadedImage( _ ))
    }).toList)

  private def get(id: Long) : Future[Option[UploadedImage]] =
    dbConfig.db.run(uploads.filter(_.id === id).result).flatMap(mapper( _ )).map(_.headOption)

  private def getAll(ids: Seq[Long]): Future[List[UploadedImage]] = {
    def compare(ids: Seq[Long], rowId: Rep[Long]) = {
      val init = ids.headOption match {
        case Some(head) => rowId === head
        case _ => rowId === -1l
      }
      ids.tail.foldLeft(init)((acc, id) => acc || rowId === id)
    }
    dbConfig.db.run(uploads.filter(row => compare(ids, row.id)).result).flatMap(mapper(_))
  }

  private def select(id: Long, email: String, parentId : Option[UUID]): Future[Boolean] = {
    val oldSelection = (for { o <- uploads if o.selected && o.email === email } yield o.selected).update(false)
    val newSelection = (for { n <- uploads if n.id === id && n.email === email } yield n.selected).update(true)

    val thumbSelection = parentId
      .map(pid => (for { n <- uploads if n.parentId === pid && n.email === email } yield n.selected).update(true))
      .getOrElse(DBIO.successful(1))
    val update = (for {
      oldDeselectCount <- oldSelection
      newSelectCount <- newSelection
      thumbSelectCount <- thumbSelection
    } yield oldDeselectCount > 0 && newSelectCount > 0).transactionally
    dbConfig.db.run(update)
  }

  override def has(email: String, width: Int, height: Int): Future[Boolean] = {
    dbConfig.db.run(uploads.filter(row =>
      row.email === email && row.selected && row.width === width && row.height === height
    ).map(_.id).result).map(_.nonEmpty)
  }

  override def getAll(email: String): Future[List[UploadedImage]] =
    dbConfig.db.run(uploads.filter(row => row.email === email && row.parentId.isEmpty).result).flatMap(mapper( _ ))

  override def getSelected(email: String, width: Int, height: Int): Future[Option[UploadedImage]] =
    dbConfig.db.run(uploads.filter(row =>
      row.email === email && row.selected && row.width === width && row.height === height
    ).result).flatMap(mapper( _ )).map(_.headOption)

  override def get(uuid: UUID, email: String): Future[Option[UploadedImage]] =
    dbConfig.db.run(uploads.filter(row => row.publicId === uuid && row.email === email).result).flatMap(mapper( _ ))
      .map(_.headOption)

  override def getThumb(uuid: UUID, width: Int, height: Int, email: String): Future[Option[UploadedImage]] = {
    dbConfig.db.run(uploads
      .filter(row => row.parentId === uuid && row.width === width && row.height === height && row.email === email).result)
      .flatMap(mapper( _ )).map(_.headOption)
  }

  override def select(uuid: UUID, email: String): Future[Boolean] = {
    dbConfig.db.run(uploads.filter(_.publicId === uuid).map(_.id).result).flatMap(_.headOption match {
      case Some(id) => this.select(id, email, Some(uuid))
      case None => Future.successful(false)
    })
  }

  override def add(image: File, fileName: String, contentType: Option[String], email: String, selected : Boolean): Future[Option[UploadedImage]] = {
    val ui = UploadedImage(selected, image, Some(fileName), contentType)
    val action = (uploads returning uploads.map(_.id)) +=
      UploadDB.unapply(UploadDB(ui, email, selected)).get

    dbConfig.db.run(action).flatMap(id => {
      this.select(id, email, Some(ui.getID)).flatMap(_ => this.get( id ))
    })
  }

  override def replaceThumbs(uuid: UUID, thumbs: List[UploadedImage], email: String): Future[Either[Exception, List[UploadedImage]]] = {
    def insert(selected: Boolean) = {
      val action = (for {
        removes <- uploads.filter(_.parentId === uuid).delete
        inserts <- uploads returning uploads.map(_.id) ++= thumbs.map((thumb) => UploadDB.unapply(UploadDB(thumb, email, selected, Some(uuid))).get)
      } yield inserts).transactionally

      dbConfig.db.run(action).flatMap(this.getAll(_)).map(Right(_))
    }
    dbConfig.db.run(uploads.filter(row => row.publicId === uuid && row.email === email).map(_.selected).result)
      .flatMap(_.headOption match {
        case Some(selected) => insert(selected)
        case _ => insert(false)
      })
  }

  override def updateEmail(previousEmail: String, newEmail: String): Future[Int] = {
    val q = for { u <- uploads if u.email === previousEmail } yield u.email
    val updateAction = q.update(newEmail)
    dbConfig.db.run(updateAction)
  }

  override def remove(uuid: UUID, email: String): Future[Int] = {
    val action = (for {
      thumbs <- uploads.filter(row => row.parentId === uuid && row.email === email).delete
      original <- uploads.filter(row => row.publicId === uuid && row.email === email).delete
    } yield thumbs + original).transactionally
    dbConfig.db.run(action)
  }
}

class RAMAvatarDao extends AvatarDao {
  var files : Map[(UUID, String), (UploadedImage, Boolean)] = Map()

  override def has(email: String, width: Int, height: Int): Future[Boolean] =
    Future.successful(this.files
      .filter(row => row._1._2 == email && row._2._2 && row._2._1.thumbnails.find(ui => ui ~ (width, height)).isDefined)
      .nonEmpty
    )

  def getAll(email: String) : Future[List[UploadedImage]] =
    Future.successful(this.files.filter(_._1._2 == email).map(_._2._1).toList)

  override def getSelected(email: String, width: Int, height: Int): Future[Option[UploadedImage]] =
    Future.successful(this.files.filter(row => row._1._2 == email && row._2._2).flatMap(
      _._2._1.thumbnails.find(ui => ui ~ (width, height))
    ).headOption)

  def get(uuid: UUID, email: String): Future[Option[UploadedImage]] =
    Future.successful(this.files.find(row => row._1._1 == uuid && row._1._2 == email).map(_._2._1))

  def getThumb(uuid: UUID, width: Int, height: Int, email: String): Future[Option[UploadedImage]] = {
    def finder(width: Int, height: Int, ui: UploadedImage) = ui ~ (width, height)
    Future.successful(this.files.find(row => row._1._1 == uuid && row._1._2 == email).flatMap(
      _._2._1.thumbnails.find((ui: UploadedImage) => finder(width, height, ui))
    ))
  }

  override def select(uuid: UUID, email: String): Future[Boolean] = ??? //{
//    this.files.filter(row => row._1._2 == email && row._2._2)
//      .foreach(row => {
//        this.files = (this.files - row._1) + ((row._1._1, row._1._2) -> (row._2._1, false))
//      })
//    val newSelection = this.files.find(row => row._1._1 == uuid && row._1._2 == email)
//      .map(row => {
//        this.files = (this.files - row._1) + ((row._1._1, row._1._2) -> (row._2._1, true))
//      }).map(_ => true).getOrElse(false)
//
//    Future.successful(newSelection)
//  }

  def add(image: File, fileName: String, contentType: Option[String], email: String, selected: Boolean): Future[Option[UploadedImage]] = ??? //{
//    val newImages = List(UploadedImage(image, Some(fileName), contentType))
//    this.files = this.files ++ newImages.map(uploaded => ((uploaded.getID, email) -> (uploaded, selected)))
//    Future.successful(newImages.headOption)
//  }

  def replaceThumbs(uuid: UUID, thumbs: List[UploadedImage], email: String): Future[Either[Exception, List[UploadedImage]]] = ??? //{
//    val id = (uuid, email)
//    this.files.get(id) match {
//      case Some(original) => {
//        this.files = (this.files - id) + (id -> (original._1.replaceThumbs(thumbs), selected))
//        Future.successful(Right(thumbs))
//      }
//      case _ => Future.successful(Left(new Exception))
//    }
//  }

  override def updateEmail(previousEmail: String, newEmail: String): Future[Int] = ??? //{
//    var counter = 0
//    this.files.filter(_._1._2 == previousEmail).foreach(entry => {
//      this.files = (this.files - entry._1) + ((entry._1._1, newEmail) -> entry._2)
//      counter += 1
//    })
//    Future.successful(counter)
//  }

  def remove(uuid: UUID, email: String) : Future[Int] = {
    var counter = 0
    this.files.filter(row => row._1._1 == uuid && row._1._2 == email).foreach(pair => {
      this.files = this.files - pair._1
      counter += 1
    })
    Future.successful(counter)
  }
}