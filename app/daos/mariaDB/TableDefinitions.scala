package daos.mariaDB

import java.net.{URI, URISyntaxException}
import java.sql.Timestamp
import java.util.Date

import slick.driver.MySQLDriver.api._
import models.{AccessRight, HttpMethod, Task}
import models.HttpMethod.HttpMethod

class AccessRightTableDef(tag: Tag) extends Table[AccessRight](tag, "AccessRight") {
  def id = column[Long]("id", O.PrimaryKey,O.AutoInc)
  def uri = column[URI]("uri")
  def method = column[HttpMethod]("method")
  def name = column[String]("name")
  def description = column[String]("description")
  def service = column[String]("service")

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
    (id, uri, method, name.?, description.?, service) <>((AccessRight.mapperTo _).tupled, AccessRight.unapply)
}

class TaskTableDef(tag: Tag) extends Table[Task](tag, "Task") {
  def id = column[Long]("id", O.PrimaryKey,O.AutoInc)
  def title = column[String]("title")
  def description= column[String]("description")
  def deadline = column[Date]("deadline")
  def count_supporter = column[Int]("count_supporter")

  implicit val dateColumnType =
    MappedColumnType .base[Date, Timestamp] (
      d => new Timestamp(d.getTime),
      d => new Date(d.getTime)
    )

  def * =
    (id, title, description.?, deadline.?, count_supporter.?) <>((Task.mapperTo _).tupled, Task.unapply)
}