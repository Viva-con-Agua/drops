package models.database

import com.mohiva.play.silhouette.api.util.PasswordInfo
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, _}

/**
  * Definition of the databas password info model
  * @param id
  * @param hasher
  * @param password
  * @param profileId
  */
case class PasswordInfoDB (
                          id: Long,
                          hasher: String,
                          password: String,
                          profileId: Long
                          ) {
  def toPasswordInfo : PasswordInfo = PasswordInfo(hasher, password)
}

object PasswordInfoDB extends ((Long, String, String, Long) => PasswordInfoDB ){
  def apply(id: Long, passwordInfo: PasswordInfo, profileId: Long): PasswordInfoDB =
    PasswordInfoDB(id, passwordInfo.hasher, passwordInfo.password, profileId)

  def apply(tuple: (Long, String, String, Long)): PasswordInfoDB =
    PasswordInfoDB(tuple._1, tuple._2, tuple._3, tuple._4)

  implicit val passwordInfoWrites : OWrites[PasswordInfoDB] = (
    (JsPath \ "id").write[Long] and
      (JsPath \ "providerId").write[String] and
      (JsPath \ "providerKey").write[String] and
      (JsPath \ "profileId").write[Long]
    )(unlift(PasswordInfoDB.unapply))

  implicit val passwordInfoReads : Reads[PasswordInfoDB] = (
    (JsPath \ "id").readNullable[Long] and
      (JsPath \ "hasher").read[String] and
      (JsPath \ "password").read[String] and
      (JsPath \ "profileId").read[Long]
    ).tupled.map((passwordInfo) => if(passwordInfo._1.isEmpty)
    PasswordInfoDB(0, passwordInfo._2, passwordInfo._3, passwordInfo._4)
  else PasswordInfoDB(passwordInfo._1.get, passwordInfo._2, passwordInfo._3, passwordInfo._4))

}


