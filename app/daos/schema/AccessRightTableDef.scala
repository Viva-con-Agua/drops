package daos.schema

import java.net.{URI, URISyntaxException}

import slick.driver.MySQLDriver.api._
import models.database.{AccessRight, HttpMethod}
import models.database.HttpMethod.HttpMethod

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
    (id, uri, method, name.?, description.?, service) <>(AccessRight.tupled, AccessRight.unapply)
}
