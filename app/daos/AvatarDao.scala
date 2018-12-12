package daos

import java.io.File
import java.sql.Blob
import java.util.UUID

import daos.schema.UploadTableDef
import models.UploadedImage
import models.database.UploadDB
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.jdbc.GetResult
import slick.driver.MySQLDriver.api._

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

trait AvatarDao {
  def getAll : Future[List[UploadedImage]]
  def get(uuid: UUID): Future[Option[UploadedImage]]
  def getThumb(uuid: UUID, width: Int, height: Int): Future[Option[UploadedImage]]

  def add(image: File, fileName: String, contentType: Option[String], email: String): Future[Option[UploadedImage]]
  def replaceThumbs(uuid: UUID, thumbs: List[UploadedImage], email: String): Future[Either[Exception, List[UploadedImage]]]
  def remove(uuid: UUID) : Future[Int]
}

class MariadbAvatarDao extends AvatarDao {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  val uploads = TableQuery[UploadTableDef]

  implicit val getUploadResult =
    GetResult(r => UploadDB(
      r.nextLong, UUID.fromString(r.nextString), r.nextStringOption().map(UUID.fromString( _ )), r.nextString,
      r.nextString, r.nextInt, r.nextInt, r.nextBlob(), r.nextString()))

  private def mapper(result : Seq[(Long, UUID, Option[UUID], String, String, Int, Int, Blob, String)]): Future[List[UploadedImage]] =
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
    dbConfig.db.run(uploads.filter((row) => compare(ids, row.id)).result).flatMap(mapper(_))
  }

  override def getAll: Future[List[UploadedImage]] =
    dbConfig.db.run(uploads.result).flatMap(mapper( _ ))

  override def get(uuid: UUID): Future[Option[UploadedImage]] =
    dbConfig.db.run(uploads.filter(_.publicId === uuid).result).flatMap(mapper( _ )).map(_.headOption)

  override def getThumb(uuid: UUID, width: Int, height: Int): Future[Option[UploadedImage]] = {
    dbConfig.db.run(uploads.filter((row) => row.parentId === uuid && row.width === width && row.height === height).result)
      .flatMap(mapper( _ )).map(_.headOption)
  }

  override def add(image: File, fileName: String, contentType: Option[String], email: String): Future[Option[UploadedImage]] = {
    val action = (uploads returning uploads.map(_.id)) +=
      UploadDB.unapply(UploadDB(UploadedImage(image, Some(fileName), contentType), email)).get

    dbConfig.db.run(action).flatMap(this.get( _ ))
  }

  override def replaceThumbs(uuid: UUID, thumbs: List[UploadedImage], email: String): Future[Either[Exception, List[UploadedImage]]] = {
    val action = (for {
      removes <- uploads.filter(_.parentId === uuid).delete
      inserts <- uploads returning uploads.map(_.id) ++= thumbs.map((thumb) => UploadDB.unapply(UploadDB(thumb, email, Some(uuid))).get)
    } yield inserts).transactionally

    dbConfig.db.run(action).flatMap(this.getAll( _ )).map(Right( _ ))
  }

  override def remove(uuid: UUID): Future[Int] = {
    val action = (for {
      thumbs <- uploads.filter(_.parentId === uuid).delete
      original <- uploads.filter(_.publicId === uuid).delete
    } yield thumbs + original).transactionally
    dbConfig.db.run(action)
  }
}

class RAMAvatarDao extends AvatarDao {
  var files : Map[(UUID, String), UploadedImage] = Map()

  def getAll : Future[List[UploadedImage]] = Future.successful(this.files.map(_._2).toList)

  def get(uuid: UUID): Future[Option[UploadedImage]] = Future.successful(this.files.find(_._1._1 == uuid).map(_._2))

  def getThumb(uuid: UUID, width: Int, height: Int): Future[Option[UploadedImage]] = {
    def finder(width: Int, height: Int, ui: UploadedImage) = ui ~ (width, height)
    Future.successful(this.files.find(_._1._1 == uuid).flatMap(
      _._2.thumbnails.find((ui: UploadedImage) => finder(width, height, ui))
    ))
  }

  def add(image: File, fileName: String, contentType: Option[String], email: String): Future[Option[UploadedImage]] = {
    val newImages = List(UploadedImage(image, Some(fileName), contentType))
    this.files = this.files ++ newImages.map(uploaded => ((uploaded.getID, email) -> uploaded))
    Future.successful(newImages.headOption)
  }

  def replaceThumbs(uuid: UUID, thumbs: List[UploadedImage], email: String): Future[Either[Exception, List[UploadedImage]]] = {
    val id = (uuid, email)
    this.files.get(id) match {
      case Some(original) => {
        this.files = (this.files - id) + (id -> original.replaceThumbs(thumbs))
        Future.successful(Right(thumbs))
      }
      case _ => Future.successful(Left(new Exception))
    }
  }

  def remove(uuid: UUID) : Future[Int] = {
    var counter = 0
    this.files.filter(_._1._1 == uuid).foreach(pair => {
      this.files = this.files - pair._1
      counter += 1
    })
    Future.successful(counter)
  }
}