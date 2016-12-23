package api

import daos.MongoUserApiQueryDao
import play.api.libs.json._

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

case class Page(start : Int, countsPerPage : Int)

case class Search(keyword: String, fields: Set[String])

case class Group(groupName: String, area : Area)

case class FilterBy(page: Option[Page], search: Option[Search], groups : Option[Set[Group]])

// Todo: Implement more potential query options (like GroupBy, etc.)

case class ApiQuery(filterBy : FilterBy) {
  private val queryDao = MongoUserApiQueryDao(this)
  def toQueryExtension : JsObject = queryDao.filter
}

object ApiQuery {
  implicit val pageFormat = Json.format[Page]
  implicit val searchFormat = Json.format[Search]
  implicit val groupFormat = Json.format[Group]
  implicit val filterByFormat = Json.format[FilterBy]
  implicit val apiQueryFormat = Json.format[ApiQuery]
}
