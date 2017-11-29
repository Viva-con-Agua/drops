package models.database

import java.util.UUID

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, _}

/**
  * Definition of the database user model
  * @param id is used as primary key and the identifier on database level
  * @param publicId is the public identifier for user
  */
case class User (
  id: Long,
  publicId: UUID
                )


object User{

  def mapperTo(
                id: Long, publicId: UUID
              ) = apply(id, publicId)

  def apply(tuple: (Long, UUID)): User =
    User(tuple._1, tuple._2)

  implicit val taskWrites : OWrites[User] = (
    (JsPath \ "id").write[Long] and
      (JsPath \ "publicId").write[UUID]
    )(unlift(User.unapply))

  implicit val taskReads : Reads[User] = (
    (JsPath \ "id").readNullable[Long] and
      (JsPath \ "publicId").read[UUID]
    ).tupled.map((user) => if(user._1.isEmpty)
    User(0, user._2)
  else User(user._1.get, user._2))
}