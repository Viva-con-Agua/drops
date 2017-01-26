package api.query.v1_1_0

import java.util.UUID

import api.query._
import daos.{CountResolver, ObjectIdResolver}
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

trait ApiQueryDao[A] {
  def filter : Future[(A, Map[String, Int])]
  def filterByPage(lastId: Option[String], countsPerPage: Int) : Future[(A, Map[String, Int])]
  def filterByGroups(groups: Set[Group]) : A
  def filterBySearch(keyword: String, fields: Set[String]): A

  def getSortCriteria : A
}

case class MongoApiQueryDao(query: ApiQuery, resolver: ObjectIdResolver with CountResolver, config : RequestConfig) extends ApiQueryDao[JsObject] {

  def filter = query.filterBy match {
    case Some(filter) => {
      val q = Map("group" -> config.filterByGroup, "search" -> config.filterBySearch).foldLeft(Json.obj())(
        (query, pair) => query ++ (pair._1 match {
          case "group" => pair._2 match {
            case true => filter.groups match {
              case Some(groups) => filterByGroups(groups)
              case _ => Json.obj()
            }
            case false => Json.obj()
          }
          case "search" => pair._2 match {
            case true => filter.search match {
              case Some(search) => Json.obj("$and" -> search.foldLeft(Json.arr())((conditions, condition) =>
                conditions :+ filterBySearch(condition.keyword, condition.fields)
              ))
              case None => Json.obj()
            }
            case false => Json.obj()
          }
        })
      )
      def pagination(q: JsObject): Future[(JsObject, Map[String, Int])] =
        if(config.filterByPage) {
          filter.page match {
            case Some(page) => {
              filterByPage(page.lastId, page.countsPerPage).map((pageQ) => (pageQ._1 ++ q, pageQ._2))
            }
            case None => Future.successful((q, Map[String, Int]()))
          }
        } else {
          Future.successful((q, Map[String, Int]()))
        }

      filter.all match {
        case Some(all) => all match {
          case true => getAll(q)
          case false => pagination(q)
        }
        case None => pagination(q)
      }
    }
    case None => Future.successful((Json.obj(), Map[String, Int]()))
  }

  def filterByGroups(groups: Set[Group]) = groups.foldLeft(Json.obj())((json, group) => json ++ filterByGroup(group))

  override def filterBySearch(keyword: String, fields: Set[String]): JsObject = Json.obj("$or" ->
    fields.foldLeft(Json.arr())((conditions, field) =>
      conditions :+ Json.obj(field -> Json.obj("$regex" -> (".*" + keyword + ".*")))
    )
  )

  def getAll(q: JsObject) : Future[(JsObject, Map[String, Int])] =
    resolver.getCount.map((i) => (q, Map("limit" -> i)))

  override def filterByPage(lastId: Option[String], countsPerPage: Int): Future[(JsObject, Map[String, Int])] =
    lastId.map((id) =>
      Try(resolver.getObjectId(UUID.fromString(id)))
        .getOrElse(resolver.getObjectId(id)).map(_ match {
        case Some(userObjId) => Json.obj("_id" -> Json.obj("$gt" -> Json.toJson(userObjId._id)))
        case None => Json.obj()
      }).map(
        (queryExtension) => (queryExtension, Map("limit" -> countsPerPage))
      )).getOrElse(Future.successful((Json.obj(), Map("limit" -> countsPerPage))))

  private def filterByGroup(group: Group) = group.area match {
    case RoleArea => Json.obj(
      "roles.role" -> group.groupName
    )
    case PillarArea => Json.obj(
      "profiles.supporter.pillars.pillar" -> group.groupName
    )
  }

  override def getSortCriteria: JsObject = config.sortBy match {
    case true => query.sortBy match {
      case Some(sortation) => sortation.foldLeft[JsObject](Json.obj())(
        (res, field) => res ++ Json.obj(field.field -> (field.dir match {
          case Asc => 1
          case Desc => -1
        }))
      )
      case None => Json.obj()
    }
    case false => Json.obj()
  }
}
