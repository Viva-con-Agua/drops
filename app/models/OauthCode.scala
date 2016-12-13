package models


import org.joda.time.{DateTime, Duration}
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, OWrites, Reads}

import scala.util.Random


/**
  * Created by johann on 02.12.16.
  */
case class OauthCode(code : String, user: User, client : OauthClient) {
  val created = new DateTime(new java.util.Date())

  // currently codes are valid for one day
  def isExpired : Boolean = created.plus(new Duration(24L*60L*60L*1000L)).isBeforeNow
}

object OauthCode {
  def apply(user: User, client: OauthClient) : OauthCode = OauthCode(Random.alphanumeric.take(100).mkString, user, client)
//  implicit val oauthCodeJsonFormat = Json.format[OauthCode]
  implicit val profileWrites : OWrites[OauthCode] = (
    (JsPath \ "code").write[String] and
      (JsPath \ "user").write[User] and
      (JsPath \ "client").write[OauthClient]
    )(unlift(OauthCode.unapply))
  implicit val profileReads : Reads[OauthCode] = (
    (JsPath \ "code").read[String] and
      (JsPath \ "user").read[User] and
      (JsPath \ "client").read[OauthClient]
    )((code, user, client) => OauthCode(code, user, client))
}