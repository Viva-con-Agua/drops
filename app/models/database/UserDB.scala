package models.database

import java.util.UUID

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, _}

/**
  * Definition of the database user model
  * @param id is used as primary key and the identifier on database level
  * @param publicId is the public identifier for user
  */
case class UserDB(
  id: Long,
  publicId: UUID
                )


object UserDB{

  def mapperTo(
                id: Long, publicId: UUID
              ) = apply(id, publicId)

  def apply(tuple: (Long, UUID)): UserDB =
    UserDB(tuple._1, tuple._2)

  implicit val userWrites : OWrites[UserDB] = (
    (JsPath \ "id").write[Long] and
      (JsPath \ "publicId").write[UUID]
    )(unlift(UserDB.unapply))

  implicit val userReads : Reads[UserDB] = (
    (JsPath \ "id").readNullable[Long] and
      (JsPath \ "publicId").read[UUID]
    ).tupled.map((user) => if(user._1.isEmpty)
    UserDB(0, user._2)
  else UserDB(user._1.get, user._2))
}