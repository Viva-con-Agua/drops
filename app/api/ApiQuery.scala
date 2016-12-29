package api

import java.util.UUID

import daos.{MongoUserApiQueryDao, UserDao}
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by johann on 21.12.16.
  */

trait Area {
  val name : String
}

object Area {
  def apply(name : String) = name match {
    case RoleArea.name => RoleArea
    case PillarArea.name => PillarArea
    case _ => throw new Exception // TODO: Use meaningful exception!
  }
  def unapply(area: Area): Option[String] = Some(area.name)

  implicit val areaJsonFormat = Json.format[Area]
}

object RoleArea extends Area {
  val name = "role"
}

object PillarArea extends Area {
  val name = "pillar"
}

case class Page(lastId : Option[UUID], countsPerPage : Int)

case class Search(keyword: String, fields: Set[String])

case class Group(groupName: String, area : Area)

case class FilterBy(page: Option[Page], search: Option[Search], groups : Option[Set[Group]])

trait Sort {
  val dir : String
}

object Sort {
  def apply(dir: String) = dir match {
    case Asc.dir => Asc
    case Desc.dir => Desc
    case _ => throw new Exception // Todo: Use meaningful exception!
  }
  def unapply(arg: Sort): Option[String] = Some(arg.dir)

  implicit val sortJsonFormat = Json.format[Sort]
}

object Asc extends Sort {
  val dir = "asc"
}

object Desc extends Sort {
  val dir = "desc"
}

case class SortField(field: String, dir: Sort)

object SortField {
  def apply(tuple: (String, String)) : SortField = SortField(tuple._1, Sort(tuple._2))

  implicit val sortFieldWrites: Writes[SortField] = (
    (__ \ 'field).write[String] and
      (__ \ 'dir).write[String]
    )(raw => (raw.field, raw.dir.dir))
  implicit val sortFieldReads : Reads[SortField] = (
    (JsPath \ "field").read[String] and
      (JsPath \ "dir").read[String]
    ).tupled.map(SortField( _ ))
}

// Todo: Implement more potential query options (like GroupBy, etc.)

case class ApiQuery(filterBy : Option[FilterBy], sortBy: Option[List[SortField]]) {
  private def queryDao(implicit userDao: UserDao) = MongoUserApiQueryDao(this, userDao)

  def toExtension(implicit userDao: UserDao) = queryDao.filter
  def toQueryExtension(implicit userDao: UserDao) : Future[JsObject] = queryDao.filter.map(_._1)
  def toLimit(implicit userDao: UserDao) : Future[Option[Int]] = queryDao.filter.map(_._2.get("limit"))

  def getSortCriteria(implicit userDao: UserDao) = queryDao.getSortCriteria
}

object ApiQuery {
  implicit val pageFormat = Json.format[Page]
  implicit val searchFormat = Json.format[Search]
  implicit val groupFormat = Json.format[Group]
  implicit val filterByFormat = Json.format[FilterBy]

  implicit val apiQueryFormat = Json.format[ApiQuery]
}
