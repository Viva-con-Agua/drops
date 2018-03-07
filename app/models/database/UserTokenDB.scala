package models.database

import java.util.UUID

import models.UserToken
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, _}

case class UserTokenDB(
                      id: UUID,
                      userId: UUID,
                      email: String,
                      expirationTime: DateTime,
                      isSignUp: Boolean
                      ){
  def toUserToken: UserToken = UserToken(id, userId, email, expirationTime, isSignUp)
}
object UserTokenDB extends ((UUID, UUID, String, DateTime, Boolean) => UserTokenDB ){
  def apply(token: UserToken):UserTokenDB =
    UserTokenDB(token.id, token.userId, token.email, token.expirationTime, token.isSignUp)

  def apply(tuple: (UUID, UUID, String, DateTime, Boolean)): UserTokenDB =
    UserTokenDB(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5)

  implicit val userTokenWrites : OWrites[UserTokenDB] = (
    (JsPath \ "id").write[UUID] and
      (JsPath \ "userId").write[UUID] and
      (JsPath \ "email").write[String] and
      (JsPath \ "expirationTime").write[DateTime] and
      (JsPath \ "isSignUp").write[Boolean]
    )(unlift(UserTokenDB.unapply))

  implicit val userTokenReads : Reads[UserTokenDB] = (
    (JsPath \ "id").read[UUID] and
      (JsPath \ "userId").read[UUID] and
      (JsPath \ "email").read[String] and
      (JsPath \ "expirationTime").read[DateTime] and
      (JsPath \ "isSignUp").read[Boolean]
    ).tupled.map((userToken) =>  UserTokenDB(userToken._1, userToken._2, userToken._3, userToken._4, userToken._5))
}
