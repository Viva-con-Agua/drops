package models

import java.net.URI

import models.HttpMethod.HttpMethod
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Created bei jottmann on 29.08.2017
  */



case class AccessRight (
                         id: Long,
                         uri: URI,
                         method: HttpMethod,
                         name: Option[String],
                         description: Option[String],
                         service: String
                      )
object AccessRight{
  def mapperTo(
                id: Long, uri: URI, method: HttpMethod,
                name: Option[String], description: Option[String], service: String
              ) = apply(id, uri, method, name, description, service)

  def apply(tuple: (Long, URI, HttpMethod, Option[String], Option[String], String)): AccessRight =
    AccessRight(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6)

  implicit val uriWrites = new Writes[URI] {
    def writes(uri: URI) = JsString(uri.toASCIIString)
  }

  implicit val uriReads = new Reads[URI] {
    override def reads(json: JsValue): JsResult[URI] =  JsSuccess(new URI(json.as[String]))
  }

  implicit val methodTypeWrite = new Writes[HttpMethod]{
    def writes(method: HttpMethod) = JsString(method.toString)
  }

  implicit val methodTypeReads = new Reads[HttpMethod] {
    //ToDo: Generic!!
    override def reads(json: JsValue): JsResult[HttpMethod] = json.as[String] match {
      case "POST" => JsSuccess(HttpMethod.POST)
      case "GET" => JsSuccess(HttpMethod.GET)
    }
  }

  implicit val accessRightWrites : OWrites[AccessRight] = (
    (JsPath \ "id").write[Long] and
      (JsPath \ "uri").write[URI] and
      (JsPath \ "method").write[HttpMethod] and
      (JsPath \ "name").writeNullable[String] and
      (JsPath \ "description").writeNullable[String] and
      (JsPath \ "service").write[String]
    )(unlift(AccessRight.unapply))

  implicit val accessRightReads : Reads[AccessRight] = (
    (JsPath \ "id").readNullable[Long] and
      (JsPath \ "uri").read[URI] and
      (JsPath \ "method").read[HttpMethod] and
      (JsPath \ "name").readNullable[String] and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "service").read[String]
    ).tupled.map((task) => if(task._1.isEmpty)
      AccessRight(0, task._2, task._3, task._4, task._5, task._6)
      else AccessRight(task._1.get, task._2, task._3, task._4, task._5, task._6))
}

object HttpMethod extends Enumeration {
  type HttpMethod = Value
  val GET, POST, DELETE, PUT = Value

  def typeToString(t : HttpMethod.Value) : String = t match {
    case HttpMethod.GET => "GET"
    case HttpMethod.POST => "POST"
    case HttpMethod.DELETE => "DELETE"
    case HttpMethod.PUT => "PUT"
  }
}
