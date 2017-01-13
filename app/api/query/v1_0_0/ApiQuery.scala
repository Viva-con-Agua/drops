package api.query.v1_0_0

import api.query._

import daos.ObjectIdResolver
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by johann on 13.01.17.
  */
case class ApiQuery(filterBy : Option[FilterBy], sortBy: Option[List[SortField]]) {
  private def queryDao(implicit resolver: ObjectIdResolver, config : RequestConfig) = MongoApiQueryDao(this, resolver, config) // Todo: Refactoring! This should be implicit by bind!

  def toExtension(implicit resolver: ObjectIdResolver, config : RequestConfig) = queryDao.filter
  def toQueryExtension(implicit resolver: ObjectIdResolver, config : RequestConfig) : Future[JsObject] = queryDao.filter.map(_._1)
  def toLimit(implicit resolver: ObjectIdResolver, config : RequestConfig) : Future[Option[Int]] = queryDao.filter.map(_._2.get("limit"))

  def getSortCriteria(implicit resolver: ObjectIdResolver, config : RequestConfig) = queryDao.getSortCriteria
}

object ApiQuery {

  implicit val pageFormat = Json.format[Page]
  implicit val searchFormat = Json.format[Search]
  implicit val groupFormat = Json.format[Group]
  implicit val filterByFormat = Json.format[FilterBy]

  implicit val apiQueryFormat = Json.format[ApiQuery]
}
