package models.database

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, _}

/**
  * Definition of the database profile model
  * @param id is used as primary key and the identifier on database level
  * @param confirmed
  * @param email
  * @param userId foreign key to define the corresponding user
  */
case class Profile (
                  id: Long,
                  confirmed: Boolean,
                  email: String,
                  userId: Long
                )


object Profile{

  def mapperTo(
                id: Long, confirmed: Boolean, email: String, userId: Long
              ) = apply(id, confirmed, email, userId)

  def apply(tuple: (Long, Boolean, String, Long)): Profile =
    Profile(tuple._1, tuple._2, tuple._3, tuple._4)

  implicit val profileWrites : OWrites[Profile] = (
    (JsPath \ "id").write[Long] and
      (JsPath \ "confirmed").write[Boolean] and
      (JsPath \ "email").write[String] and
      (JsPath \ "userId").write[Long]
    )(unlift(Profile.unapply))

  implicit val profileReads : Reads[Profile] = (
    (JsPath \ "id").readNullable[Long] and
      (JsPath \ "confirmed").read[Boolean] and
      (JsPath \ "email").read[String] and
      (JsPath \ "userId").read[Long]
    ).tupled.map((profile) => if(profile._1.isEmpty)
    Profile(0, profile._2, profile._3, profile._4)
  else Profile(profile._1.get, profile._2, profile._3, profile._4))
}