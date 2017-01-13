package api

import api.query.RequestConfig
import daos.{CountResolver, ObjectIdResolver}
import play.api.libs.json.JsObject

import scala.concurrent.Future

trait ApiQuery[A] {

  def toExtension(implicit resolver: ObjectIdResolver with CountResolver, config : RequestConfig) : Future[(A, Map[String, Int])]
  def toQueryExtension(implicit resolver: ObjectIdResolver with CountResolver, config : RequestConfig) : Future[JsObject]
  def toLimit(implicit resolver: ObjectIdResolver with CountResolver, config : RequestConfig) : Future[Option[Int]]

  def getSortCriteria(implicit resolver: ObjectIdResolver with CountResolver, config : RequestConfig) : A
}
