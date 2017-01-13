package api.query.v1_1_0

import api.query._
import daos.{CountResolver, ObjectIdResolver}
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by johann on 13.01.17.
  */
case class ApiQuery(filterBy : Option[FilterBy], sortBy: Option[List[SortField]]) extends api.ApiQuery[JsObject] {
  private def queryDao(implicit resolver: ObjectIdResolver with CountResolver, config : RequestConfig) =
    MongoApiQueryDao(this, resolver, config) // Todo: Refactoring! This should be implicit by bind!

  def toExtension(implicit resolver: ObjectIdResolver with CountResolver, config : RequestConfig) : Future[(JsObject, Map[String, Int])] = queryDao.filter

  def toQueryExtension(implicit resolver: ObjectIdResolver with CountResolver, config : RequestConfig) : Future[JsObject] =
    queryDao.filter.map(_._1)

  def toLimit(implicit resolver: ObjectIdResolver with CountResolver, config : RequestConfig) : Future[Option[Int]] =
    queryDao.filter.map(_._2.get("limit"))

  def getSortCriteria(implicit resolver: ObjectIdResolver with CountResolver, config : RequestConfig) : JsObject =
    queryDao.getSortCriteria
}

object ApiQuery {

  implicit val pageFormat = Json.format[Page]
  implicit val searchFormat = Json.format[Search]
  implicit val groupFormat = Json.format[Group]
  implicit val filterByFormat = Json.format[FilterBy]

  implicit val apiQueryFormat = Json.format[ApiQuery]
}
