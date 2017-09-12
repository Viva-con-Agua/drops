package daos

import java.net.{URI, URISyntaxException}
import javax.inject.Inject

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._
import models.{AccessRight, HttpMethod}
import models.HttpMethod.HttpMethod

/**
  * Created bei jottmann on 29.08.2017
  */

trait AccessRightDao {
  def all(): Future[Seq[AccessRight]]
  def find(id:Long): Future[Option[AccessRight]]
  def create(accessRight:AccessRight): Future[Option[AccessRight]]
  def delete(id:Long): Future[Int]
}

class AccessRightTableDef(tag: Tag) extends Table[AccessRight](tag, "AccessRight") {
  def id = column[Long]("id", O.PrimaryKey,O.AutoInc)
  def uri = column[URI]("uri")
  def method = column[HttpMethod]("method")
  def name = column[String]("name")
  def description= column[String]("description")

  implicit val uriColumnType = MappedColumnType.base[URI, String](
    { uri => uri.toASCIIString },
    { str =>
      try {
        new URI(str)
      } catch {
        case e:URISyntaxException => throw new SlickException(s"Invalid URI value: $str", e)
      }
    }
  )

  implicit val httpMethodColumnType = MappedColumnType.base[HttpMethod, String](
    {httpMethod => httpMethod.toString},
    {
      case "POST" => HttpMethod.POST
      case "GET" => HttpMethod.GET
    }
  )

  def * =
    (id, uri, method, name.?, description.?) <>((AccessRight.mapperTo _).tupled, AccessRight.unapply)
}

class MariadbAccessRightDao @Inject()(dbConfigProvider: DatabaseConfigProvider) extends AccessRightDao {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  val accessRights = TableQuery[AccessRightTableDef]

  def all(): Future[Seq[AccessRight]] = dbConfig.db.run(accessRights.result)

  def find(id: Long): Future[Option[AccessRight]] = dbConfig.db.run(accessRights.filter(ar => ar.id === id).result).map(_.headOption)

  def create(accessRight: AccessRight): Future[Option[AccessRight]] = {
    dbConfig.db.run((accessRights returning accessRights.map(_.id)) += accessRight).flatMap((ac)=> find(ac))
  }

  def delete(id: Long): Future[Int] = dbConfig.db.run(accessRights.filter(ar => ar.id === id).delete)
}