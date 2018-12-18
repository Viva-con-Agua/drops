package models.database

import java.util.UUID

import models.{Crew, Profile, Role, User}
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, _}

/**
  * Definition of the database user model
  * @param id is used as primary key and the identifier on database level
  * @param publicId is the public identifier for user
  */
case class UserDB(
  id: Long,
  publicId: UUID,
  roles: String,
  updated: Long,
  created: Long
                ) {
  def toUser(profiles: Seq[Profile]) : User =
    User(publicId, profiles.toList, updated, created, roles.split(",").map(Role( _ )).toSet)
}


object UserDB extends ((Long, UUID, String, Long, Long) => UserDB ){
  def apply(tuple: (Long, UUID, String, Long, Long)): UserDB =
    UserDB(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5)

  def apply(user: User): UserDB={
    var roles = Set[String]()
    user.roles.foreach(role =>
      roles += role.name
    )
    UserDB(0, user.id, roles.mkString(","), user.updated, user.created)
  }

  def read(entries : Seq[(UserDB, ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[Crew])]) : Seq[User] = {
    val sorted = entries.foldLeft[Map[UserDB, Seq[(ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[Crew])]]](Map())(
      (mapped, entry) =>
        mapped.contains(entry._1) match {
          case true => mapped.get(entry._1) match {
            case Some(seq) => (mapped - entry._1) + (entry._1 -> (seq ++ Seq((entry._2, entry._3, entry._4, entry._5, entry._6, entry._7, entry._8))))
            case None => (mapped - entry._1) + (entry._1 -> Seq((entry._2, entry._3, entry._4, entry._5, entry._6, entry._7, entry._8)))
          }
          case false => mapped + (entry._1 -> Seq((entry._2, entry._3, entry._4, entry._5, entry._6, entry._7, entry._8)))
        }).toSeq

    sorted.map(pair => {
      pair._1.toUser(ProfileDB.read(pair._2))
    })
  }

  implicit val userWrites : OWrites[UserDB] = (
    (JsPath \ "id").write[Long] and
      (JsPath \ "publicId").write[UUID] and
      (JsPath \ "roles").write[String] and
      (JsPath \ "updated").write[Long] and
      (JsPath \ "created").write[Long]
    )(unlift(UserDB.unapply))

  implicit val userReads : Reads[UserDB] = (
    (JsPath \ "id").readNullable[Long] and
      (JsPath \ "publicId").read[UUID] and
      (JsPath \ "roles").read[String] and
      (JsPath \ "updated").readNullable[Long] and
      (JsPath \ "created").readNullable[Long]
    ).tupled.map((user) => if(user._1.isEmpty)
    UserDB(0, user._2, user._3, user._4.getOrElse(System.currentTimeMillis), user._5.getOrElse(System.currentTimeMillis))
  else UserDB(user._1.get, user._2, user._3, user._4.getOrElse(System.currentTimeMillis), user._5.getOrElse(System.currentTimeMillis)))
}