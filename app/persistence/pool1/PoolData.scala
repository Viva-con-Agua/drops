package persistence.pool1

import play.api.http._
import play.api.mvc._
import models.User
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, JsValue, OWrites, Reads, Json}
import scala.concurrent.ExecutionContext.Implicits.global

trait PoolData[T] {
  def toPost : JsValue
}

object PoolData {
  /**
    * Needed to use Plays [[request.post(data)]] for PoolData[T].
    *
    * @param codec the used codec
    * @tparam T the corresponding type of the [[PoolData]] instance
    * @return
    */
  implicit def writeable[T](implicit codec: Codec): Writeable[PoolData[T]] = {
    Writeable(data => codec.encode(data.toPost.toString()))
  }

  implicit def contentType[T](implicit codec: Codec): ContentTypeOf[PoolData[T]] = {
    // for text/plain
    ContentTypeOf(Some(ContentTypes.TEXT))
  }
}

case class PoolUserData(user: User) extends PoolData[User] {
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

    implicit val userMetaWrites : OWrites[UserMeta] = (
      (JsPath \ "nickName").write[String] and
        (JsPath \ "firstName").write[String] and
        (JsPath \ "lastName").write[String] and
        (JsPath \ "mobile").write[String] and
        (JsPath \ "residence").write[String] and
        (JsPath \ "birthday").write[Long] and
        (JsPath \ "gender").write[String]
      )(unlift(UserMeta.unapply))

    implicit val userMetaReads : Reads[UserMeta] = (
      (JsPath \ "nickName").read[String] and
        (JsPath \ "firstName").read[String] and
        (JsPath \ "lastName").read[String] and
        (JsPath \ "mobile").read[String] and
        (JsPath \ "residence").read[String] and
        (JsPath \ "birthday").read[Long] and
        (JsPath \ "gender").read[String]
      ).tupled.map(UserMeta( _ ))
  }

  case class PoolUserDataContainer(userLogin : String, userNiceName: String, email: String, displayName : String, userName : String, meta: UserMeta)

  object PoolUserDataContainer {
    def apply(container: (String, String, String, String, String, UserMeta)) : PoolUserDataContainer = container match {
      case (userLogin, userNiceName, email, displayName, userName, meta) =>
        PoolUserDataContainer(userLogin, userNiceName, email, displayName, userName, meta)
    }

    implicit val containerWrites : OWrites[PoolUserDataContainer] = (
      (JsPath \ "userLogin").write[String] and
        (JsPath \ "userNiceName").write[String] and
        (JsPath \ "email").write[String] and
        (JsPath \ "displayName").write[String] and
        (JsPath \ "userName").write[String] and
        (JsPath \ "meta").write[UserMeta]
      )(unlift(PoolUserDataContainer.unapply))

    implicit val containerReads : Reads[PoolUserDataContainer] = (
      (JsPath \ "userLogin").read[String] and
        (JsPath \ "userNiceName").read[String] and
        (JsPath \ "email").read[String] and
        (JsPath \ "displayName").read[String] and
        (JsPath \ "userName").read[String] and
        (JsPath \ "meta").read[UserMeta]
      ).tupled.map(PoolUserDataContainer( _ ))
  }

  /**
    * Mapping between User and PoolUser
    */
  val container = PoolUserDataContainer(
    userLogin = user.profiles.head.supporter.username.getOrElse(""),
    userNiceName = user.profiles.head.supporter.username.getOrElse(""),
    email = user.profiles.head.email.getOrElse(""),
    displayName = user.profiles.head.supporter.fullName.getOrElse(""),
    userName = user.profiles.head.supporter.username.getOrElse(""),
    meta = UserMeta(
      nickName = user.profiles.head.supporter.username.getOrElse(""),
      firstName = user.profiles.head.supporter.firstName.getOrElse(""),
      lastName = user.profiles.head.supporter.lastName.getOrElse(""),
      mobile = user.profiles.head.supporter.mobilePhone.getOrElse(""),
      residence = user.profiles.head.supporter.placeOfResidence.getOrElse(""),
      birthday = user.profiles.head.supporter.birthday.get,
      gender = user.profiles.head.supporter.sex.getOrElse("")
    )
  )
  override def toPost: JsValue = Json.toJson(this.container)
}