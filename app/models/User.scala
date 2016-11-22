package models

import java.text.SimpleDateFormat
import java.util.{Calendar, Date, UUID}

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, OWrites, Reads}
import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.providers.OAuth1Info
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import play.api.i18n.Messages

case class Supporter(
  firstName: Option[String],
  lastName: Option[String],
  fullName: Option[String],
  username: Option[String],
  mobilePhone: Option[String],
  placeOfResidence: Option[String],
  birthday: Option[Long],
  sex: Option[String],
  crew: Option[Crew]
) {
  def birthString(implicit messages: Messages): Option[String] = this.birthday match {
    case Some(l) => {
      val now = Calendar.getInstance()
      now.setTimeInMillis(l)
      val format = new SimpleDateFormat(Messages("date.format"))
      Some(format.format(now.getTime()))
    }
    case _ => None
  }
}

object Supporter {
  def apply(firstName: String, lastName: String, mobilePhone: String, placeOfResidence: String, birthday: Date, sex : String) : Supporter =
    Supporter(Some(firstName), Some(lastName), Some(s"${firstName} ${lastName}"), None, Some(mobilePhone), Some(placeOfResidence), Some(birthday.getTime()), Some(sex), None)

  def apply(firstName: Option[String], lastName: Option[String], fullName: Option[String]) : Supporter =
    Supporter(firstName, lastName, fullName, None, None, None, None, None, None)

  def apply(tuple: (Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[Long], Option[String], Option[Crew])) : Supporter =
    Supporter(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7, tuple._8, tuple._9)

  implicit val supporterWrites : OWrites[Supporter] = (
    (JsPath \ "firstName").writeNullable[String] and
      (JsPath \ "lastName").writeNullable[String] and
      (JsPath \ "fullName").writeNullable[String] and
      (JsPath \ "username").writeNullable[String] and
      (JsPath \ "mobilePhone").writeNullable[String] and
      (JsPath \ "placeOfResidence").writeNullable[String] and
      (JsPath \ "birthday").writeNullable[Long] and
      (JsPath \ "sex").writeNullable[String] and
      (JsPath \ "crew").writeNullable[Crew]
    )(unlift(Supporter.unapply))
  implicit val supporterReads : Reads[Supporter] = (
      (JsPath \ "firstName").readNullable[String] and
      (JsPath \ "lastName").readNullable[String] and
      (JsPath \ "fullName").readNullable[String] and
      (JsPath \ "username").readNullable[String] and
      (JsPath \ "mobilePhone").readNullable[String] and
      (JsPath \ "placeOfResidence").readNullable[String] and
      (JsPath \ "birthday").readNullable[Long] and
      (JsPath \ "sex").readNullable[String] and
      (JsPath \ "crew").readNullable[Crew]
    ).tupled.map(Supporter( _ ))
}

case class Profile(
  loginInfo:LoginInfo,
  confirmed: Boolean,
  email:Option[String],
  supporter: Supporter,
  passwordInfo:Option[PasswordInfo], 
  oauth1Info: Option[OAuth1Info],
  avatarUrl: Option[String]) {
}

object Profile {

  def apply(loginInfo : LoginInfo, email: String, firstName: String, lastName: String, mobilePhone: String, placeOfResidence: String, birthday: Date, sex : String) : Profile =
    // confirmation is false by default, because this apply function is designed for using it during the default sign up process
    Profile(loginInfo, false, Some(email), Supporter(firstName, lastName, mobilePhone, placeOfResidence, birthday, sex), None, None, None)

  def apply(tuple: (LoginInfo, Boolean, Option[String], Supporter, Option[PasswordInfo], Option[OAuth1Info], Option[String])) : Profile =
    Profile(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7)

  def apply(p: CommonSocialProfile) : Profile =
    Profile(p.loginInfo, true, p.email, Supporter(p.firstName, p.lastName, p.fullName), None, None, p.avatarURL)

  implicit val passwordInfoJsonFormat = Json.format[PasswordInfo]
  implicit val oauth1InfoJsonFormat = Json.format[OAuth1Info]
  implicit val profileWrites : OWrites[Profile] = (
    (JsPath \ "loginInfo").write[LoginInfo] and
      (JsPath \ "confirmed").write[Boolean] and
      (JsPath \ "email").writeNullable[String] and
      (JsPath \ "supporter").write[Supporter] and
      (JsPath \ "passwordInfo").writeNullable[PasswordInfo] and
      (JsPath \ "oauth1Info").writeNullable[OAuth1Info] and
      (JsPath \ "avatarUrl").writeNullable[String]
    )(unlift(Profile.unapply))
  implicit val profileReads : Reads[Profile] = (
      (JsPath \ "loginInfo").read[LoginInfo] and
      (JsPath \ "confirmed").read[Boolean] and
      (JsPath \ "email").readNullable[String] and
      (JsPath \ "supporter").read[Supporter] and
      (JsPath \ "passwordInfo").readNullable[PasswordInfo] and
      (JsPath \ "oauth1Info").readNullable[OAuth1Info] and
      (JsPath \ "avatarUrl").readNullable[String]
    ).tupled.map(Profile( _ ))
}

case class User(id: UUID, profiles: List[Profile]) extends Identity {
  def updateProfile(updatedProfile: Profile) = User(this.id, profiles.map(p => p.loginInfo match {
    case updatedProfile.loginInfo => updatedProfile
    case _ => p
  }))
  def profileFor(loginInfo:LoginInfo) = profiles.find(_.loginInfo == loginInfo)
  def fullName(loginInfo:LoginInfo) = profileFor(loginInfo).flatMap(_.supporter.fullName)
}

object User {
  implicit val passwordInfoJsonFormat = Json.format[PasswordInfo]
  implicit val oauth1InfoJsonFormat = Json.format[OAuth1Info]
  implicit val userJsonFormat = Json.format[User]
}
