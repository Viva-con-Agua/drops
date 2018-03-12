package persistence.pool1

import play.api.http._
import play.api.mvc._
import models.User
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, JsValue, OWrites, Reads, Json}
import scala.concurrent.ExecutionContext.Implicits.global

trait PoolData[T] {
  val hash : String
  val content: JsValue
  val paramName : String
  def toPost : Map[String, Seq[String]]
}

object PoolData {
  /**
    * Needed to use Plays `request.post(data)` for PoolData[T].
    *
    * @param codec the used codec
    * @tparam T the corresponding type of the [[PoolData]] instance
    * @return
    */
  implicit def writeable[T](implicit codec: Codec): Writeable[PoolData[T]] = {
    Writeable(data => {
      codec.encode("hash=" + data.hash + "&" + data.paramName + "=" + data.content)
    })
  }

  implicit def contentType[T](implicit codec: Codec): ContentTypeOf[PoolData[T]] = {
    // for text/plain
    ContentTypeOf(Some(ContentTypes.TEXT))
  }
}

case class PoolUserData(override val hash: String, user: User) extends PoolData[User] {
  case class UserMeta(nickName: String, firstName: String, lastName: String, mobile: String, residence: String, birthday: Long, gender: String) {
    /**
      * Since Pool 1 has a special geographic management, we have to set the ID's of the Pool 1 database.
      */
    val nation = 40
    val city = 1
    val region = 1
  }

  object UserMeta {

    def apply(meta: (String, String, String, String, String, Long, String)) : UserMeta = meta match {
      case (nickName, firstName, lastName, mobile, residence, birthday, gender) =>
        UserMeta(nickName, firstName, lastName, mobile, residence, birthday, gender)
    }

    def apply(meta: (String, String, String, String, String, Long, String, Int, Int, Int)) : UserMeta =
      UserMeta(meta._1, meta._2, meta._3, meta._4, meta._5, meta._6, meta._7)

    def unfold(m: UserMeta) : Option[(String, String, String, String, String, Long, String, Int, Int, Int)] =
      Some((m.nickName, m.firstName, m.lastName, m.mobile, m.residence, m.birthday, m.gender, m.nation, m.city, m.region))

    implicit val userMetaWrites : OWrites[UserMeta] = (
      (JsPath \ "nickname").write[String] and
        (JsPath \ "first_name").write[String] and
        (JsPath \ "last_name").write[String] and
        (JsPath \ "mobile").write[String] and
        (JsPath \ "residence").write[String] and
        (JsPath \ "birthday").write[Long] and
        (JsPath \ "gender").write[String] and
        (JsPath \ "nation").write[Int] and
        (JsPath \ "city").write[Int] and
        (JsPath \ "region").write[Int]
      )(unlift(UserMeta.unfold))

    implicit val userMetaReads : Reads[UserMeta] = (
      (JsPath \ "nickname").read[String] and
        (JsPath \ "first_name").read[String] and
        (JsPath \ "last_name").read[String] and
        (JsPath \ "mobile").read[String] and
        (JsPath \ "residence").read[String] and
        (JsPath \ "birthday").read[Long] and
        (JsPath \ "gender").read[String] and
        (JsPath \ "nation").read[Int] and
        (JsPath \ "city").read[Int] and
        (JsPath \ "region").read[Int]
      ).tupled.map(UserMeta( _ ))
  }

  case class PoolUserDataContainer(userLogin : String, userNiceName: String, email: String, displayName : String, userName : String, meta: UserMeta)

  object PoolUserDataContainer {
    def apply(container: (String, String, String, String, String, UserMeta)) : PoolUserDataContainer = container match {
      case (userLogin, userNiceName, email, displayName, userName, meta) =>
        PoolUserDataContainer(userLogin, userNiceName, email, displayName, userName, meta)
    }

    implicit val containerWrites : OWrites[PoolUserDataContainer] = (
      (JsPath \ "user_login").write[String] and
        (JsPath \ "user_nicename").write[String] and
        (JsPath \ "user_email").write[String] and
        (JsPath \ "display_name").write[String] and
        (JsPath \ "userName").write[String] and
        (JsPath \ "usermeta").write[UserMeta]
      )(unlift(PoolUserDataContainer.unapply))

    implicit val containerReads : Reads[PoolUserDataContainer] = (
      (JsPath \ "user_login").read[String] and
        (JsPath \ "user_nicename").read[String] and
        (JsPath \ "user_email").read[String] and
        (JsPath \ "display_name").read[String] and
        (JsPath \ "userName").read[String] and
        (JsPath \ "usermeta").read[UserMeta]
      ).tupled.map(PoolUserDataContainer( _ ))
  }

  /**
    * Mapping between User and PoolUser
    */
  val container = PoolUserDataContainer(
    userLogin = user.profiles.head.email.getOrElse(""),
    userNiceName = user.profiles.head.supporter.fullName.getOrElse(""),
    email = user.profiles.head.email.getOrElse(""),
    displayName = user.profiles.head.supporter.fullName.getOrElse(""),
    userName = user.profiles.head.supporter.fullName.getOrElse(""),
    meta = UserMeta(
      nickName = user.profiles.head.supporter.fullName.getOrElse(""),
      firstName = user.profiles.head.supporter.firstName.getOrElse(""),
      lastName = user.profiles.head.supporter.lastName.getOrElse(""),
      mobile = user.profiles.head.supporter.mobilePhone.getOrElse(""),
      residence = user.profiles.head.supporter.placeOfResidence.getOrElse(""),
      birthday = user.profiles.head.supporter.birthday.get,
      gender = user.profiles.head.supporter.sex.getOrElse("")
    )
  )
  override val content: JsValue = Json.toJson(this.container)
  override val paramName : String = "user"

  override def toPost : Map[String, Seq[String]] = Map(
    "hash" -> Seq(hash),
    "user" -> Seq(content.toString())
  )
}