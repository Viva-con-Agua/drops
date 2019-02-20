package persistence.pool1

import java.util.UUID

import models.User
import persistence.pool1.PoolRequestUUIDData.UUIDWrapper
import play.api.libs.functional.syntax._
import play.api.libs.json._

trait PoolRequest {
  val hash: String
  def toPost: JsValue
}

object PoolRequest {
  def create(hash: String, user: User): Option[PoolRequestUserData] =
    PoolRequestUser.create(user).map(rq => PoolRequestUserData(hash, rq))

  def read(hash: String, id: UUID): Option[PoolRequestUUIDData] =
    Some(PoolRequestUUIDData(hash, UUIDWrapper(id)))

  def read(hash: String, user: User): Option[PoolRequestUUIDData] =
    read(hash, user.id)

  def update(hash: String, user: User, mailSwitch: Option[String]): Option[PoolRequestUserData] =
    PoolRequestUser.update(user, mailSwitch).map(rq => PoolRequestUserData(hash, rq))

  def delete(hash: String, id: UUID): Option[PoolRequestUUIDData] =
    Some(PoolRequestUUIDData(hash, UUIDWrapper(id)))

  def delete(hash: String, user: User): Option[PoolRequestUUIDData] =
    delete(hash, user.id)

  def logout(hash: String, id: UUID): Option[PoolRequestUUIDData] =
    Some(PoolRequestUUIDData(hash, UUIDWrapper(id)))

  def logout(hash: String, user: User): Option[PoolRequestUUIDData] =
    logout(hash, user.id)
}

case class PoolRequestUserData(hash: String, user: PoolRequestUser) extends PoolRequest {
  override def toPost: JsValue = PoolRequestUserData.toPost(this)
}
object PoolRequestUserData {

  implicit val poolRequestUserDataWrites : OWrites[PoolRequestUserData] = (
    (JsPath \ "hash").write[String] and
      (JsPath \ "user").write[PoolRequestUser]
    )(unlift(PoolRequestUserData.unapply))
  implicit val poolRequestUserDataReads : Reads[PoolRequestUserData] = (
    (JsPath \ "hash").read[String] and
      (JsPath \ "user").read[PoolRequestUser]
    ).tupled.map(t => PoolRequestUserData( t._1, t._2 ))

  def toPost(rq : PoolRequestUserData): JsValue = Json.toJson(rq)
}

case class PoolRequestUUIDData(hash: String, user: UUIDWrapper) extends PoolRequest {
  override def toPost: JsValue = Json.toJson(this)
}
object PoolRequestUUIDData {

  case class UUIDWrapper(uuid: UUID)
  object UUIDWrapper {
    implicit val poolUUIDWrapper = Json.format[UUIDWrapper]
  }

  implicit val poolRequestUUIDDataWrites : OWrites[PoolRequestUUIDData] = (
    (JsPath \ "hash").write[String] and
      (JsPath \ "user").write[UUIDWrapper]
    )(unlift(PoolRequestUUIDData.unapply))
  implicit val poolRequestUUIDDataReads : Reads[PoolRequestUUIDData] = (
    (JsPath \ "hash").read[String] and
      (JsPath \ "user").read[UUIDWrapper]
    ).tupled.map(t => PoolRequestUUIDData( t._1, t._2 ))
}

case class PoolRequestUser(
                            uuid: Option[UUID],
                            user_login: String, //email
                            user_nicename: String, //full_name
                            user_email: String,
                            display_name: String, //full_name
                            nickname: Option[String], //full_name
                            first_name: Option[String],
                            last_name: Option[String],
                            mobile: Option[String],
                            residence: Option[String],
                            birthday: Option[String],
                            mail_switch: Option[String],
                            gender: Option[String],
                            nation: Option[String], // cities head country
                            city: Option[String], // crew name
                            region: Option[String], // empty
                            crew_id: Option[UUID]
                          )

object PoolRequestUser {
  def apply(user: User, withUUID: Boolean, mail_switch: Option[String]): Option[PoolRequestUser] = user.profiles.headOption.flatMap(profile => profile.email.map(email =>
    PoolRequestUser(
      uuid = withUUID match {
        case true => Some(user.id)
        case false => None
      },
      user_login = email,
      user_nicename = profile.supporter.fullName.getOrElse(email),
      user_email = email,
      display_name = profile.supporter.fullName.getOrElse(email),
      nickname = profile.supporter.fullName,
      first_name = profile.supporter.firstName,
      last_name = profile.supporter.lastName,
      mobile = profile.supporter.mobilePhone,
      residence = profile.supporter.placeOfResidence,
      birthday = profile.supporter.birthday.map(_.toString),
      mail_switch = mail_switch,
      gender = profile.supporter.sex,
      nation = profile.supporter.crew.flatMap(_.cities.headOption).map(_.country),
      city = profile.supporter.crew.map(_.name),
      region = Some(""),
      crew_id = profile.supporter.crew.map(_.id)
    )
  ))
  def create(user: User) : Option[PoolRequestUser] = PoolRequestUser(user, false, None)
  def update(user: User, mail_switch: Option[String]) : Option[PoolRequestUser] = PoolRequestUser(user, true, mail_switch)

  implicit val poolRequestUserWrites : OWrites[PoolRequestUser] = (
    (JsPath \ "uuid").writeNullable[UUID] and
      (JsPath \ "user_login").write[String] and
      (JsPath \ "user_nicename").write[String] and
      (JsPath \ "user_email").write[String] and
      (JsPath \ "display_name").write[String] and
      (JsPath \ "nickname").writeNullable[String] and
      (JsPath \ "first_name").writeNullable[String] and
      (JsPath \ "last_name").writeNullable[String] and
      (JsPath \ "mobile").writeNullable[String] and
      (JsPath \ "residence").writeNullable[String] and
      (JsPath \ "birthday").writeNullable[String] and
      (JsPath \ "mail_switch").writeNullable[String] and
      (JsPath \ "gender").writeNullable[String] and
      (JsPath \ "nation").writeNullable[String] and
      (JsPath \ "city").writeNullable[String] and
      (JsPath \ "region").writeNullable[String] and
      (JsPath \ "crew_id").writeNullable[UUID]
    )(unlift(PoolRequestUser.unapply))
  implicit val poolRequestUserReads : Reads[PoolRequestUser] = (
    (JsPath \ "uuid").readNullable[UUID] and
      (JsPath \ "user_login").read[String] and
      (JsPath \ "user_nicename").read[String] and
      (JsPath \ "user_email").read[String] and
      (JsPath \ "display_name").read[String] and
      (JsPath \ "nickname").readNullable[String] and
      (JsPath \ "first_name").readNullable[String] and
      (JsPath \ "last_name").readNullable[String] and
      (JsPath \ "mobile").readNullable[String] and
      (JsPath \ "residence").readNullable[String] and
      (JsPath \ "birthday").readNullable[String] and
      (JsPath \ "mail_switch").readNullable[String] and
      (JsPath \ "gender").readNullable[String] and
      (JsPath \ "nation").readNullable[String] and
      (JsPath \ "city").readNullable[String] and
      (JsPath \ "region").readNullable[String] and
      (JsPath \ "crew_id").readNullable[UUID]
    ).tupled.map(t => PoolRequestUser(
        t._1, t._2, t._3, t._4, t._5, t._6, t._6, t._8, t._9, t._10, t._11, t._12, t._13, t._14, t._15, t._16, t._17
    ))
}