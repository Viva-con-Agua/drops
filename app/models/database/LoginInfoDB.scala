package models.database

import com.mohiva.play.silhouette.api.LoginInfo
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, _}

/**
  * Definition of the database login info model
  * @param id
  * @param providerId
  * @param providerKey
  * @param profileId
  */
case class LoginInfoDB(
                       id: Long,
                       providerId: String,
                       providerKey: String,
                       profileId: Long
                     ) {
  def toLoginInfo: LoginInfo = LoginInfo(providerId, providerKey)

}

object LoginInfoDB extends ((Long, String, String, Long) => LoginInfoDB ){
  def apply(id: Long, loginInfo: LoginInfo, profileId: Long): LoginInfoDB =
    LoginInfoDB(id, loginInfo.providerID, loginInfo.providerKey, profileId)

  def apply(tuple: (Long, String, String, Long)): LoginInfoDB =
    LoginInfoDB(tuple._1, tuple._2, tuple._3, tuple._4)

  implicit val loginInfoWrites : OWrites[LoginInfoDB] = (
    (JsPath \ "id").write[Long] and
      (JsPath \ "providerId").write[String] and
      (JsPath \ "providerKey").write[String] and
      (JsPath \ "profileId").write[Long]
  )(unlift(LoginInfoDB.unapply))

  implicit val loginInfoReads : Reads[LoginInfoDB] = (
    (JsPath \ "id").readNullable[Long] and
      (JsPath \ "providerId").read[String] and
      (JsPath \ "providerKey").read[String] and
      (JsPath \ "profileId").read[Long]
    ).tupled.map((loginInfo) => if(loginInfo._1.isEmpty)
    LoginInfoDB(0, loginInfo._2, loginInfo._3, loginInfo._4)
  else LoginInfoDB(loginInfo._1.get, loginInfo._2, loginInfo._3, loginInfo._4))
}
