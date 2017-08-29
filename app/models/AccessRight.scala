package models

import java.net.URI

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}

/**
  * Created bei jottmann on 29.08.2017
  */

case class AccessRight(
                      uri: URI,
                      name: Option[String],
                      description: Option[String]
                      )
object AccessRight {
  def apply(tuple: (URI, Option[String], Option[String])): AccessRight =
    AccessRight(tuple._1, tuple._2, tuple._3)

  def apply(uri: URI, name: String, description: String): AccessRight =
    AccessRight(uri, Some(name), Some(description))

  //ToDo: WIP
  /*implicit val accessRightReads : Reads[AccessRight] = (
    (JsPath \ "uri").read[URI] and
      (JsPath \ "name").readNullable[String] and
      (JsPath \ "description").readNullable[String]
    ).tupled.map(AccessRight( _ ))
  */
}
