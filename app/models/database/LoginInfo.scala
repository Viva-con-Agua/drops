package models.database

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, _}

/**
  * Definition of the database login info model
  * @param id
  * @param providerId
  * @param providerKey
  * @param profileId
  */
case class LoginInfo (
                       id: Long,
                       providerId: String,
                       providerKey: String,
                       profileId: Long
                     )

object LoginInfo{
  def mapperTo(
              id: Long, providerId: String, providerKey: String, profileId: Long
              ) = apply(id, providerId, providerKey, profileId)

  def apply(tuple: (Long, String, String, Long)): LoginInfo =
    LoginInfo(tuple._1, tuple._2, tuple._3, tuple._4)

  implicit val loginInfoWrites : OWrites[LoginInfo] = (
    (JsPath \ "id").write[Long] and
      (JsPath \ "providerId").write[String] and
      (JsPath \ "providerKey").write[String] and
      (JsPath \ "profileId").write[Long]
  )(unlift(LoginInfo.unapply))

  implicit val loginInfoReads : Reads[LoginInfo] = (
    (JsPath \ "id").readNullable[Long] and
      (JsPath \ "providerId").read[String] and
      (JsPath \ "providerKey").read[String] and
      (JsPath \ "profileId").read[Long]
    ).tupled.map((loginInfo) => if(loginInfo._1.isEmpty)
    LoginInfo(0, loginInfo._2, loginInfo._3, loginInfo._4)
  else LoginInfo(loginInfo._1.get, loginInfo._2, loginInfo._3, loginInfo._4))
}
