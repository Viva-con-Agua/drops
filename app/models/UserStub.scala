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

case class SupporterStub(
                      firstName: Option[String],
                      lastName: Option[String],
                      fullName: Option[String],
                      mobilePhone: Option[String],
                      placeOfResidence: Option[String],
                      birthday: Option[Long],
                      sex: Option[String],
                      crew: Option[CrewStub],
                      roles: Set[Role],
                      pillars: Set[Pillar],
                      address: Set[AddressStub],
                      active: Option[String],
                      nvmDate: Option[Long]
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

  def toSupporter(c: Option[Crew], address: Set[Address]) : Supporter =
    Supporter(firstName, lastName, fullName, mobilePhone, placeOfResidence, birthday, sex, c, roles, pillars, address, active, nvmDate)
}

object SupporterStub {
  def apply(firstName: String, lastName: String, mobilePhone: String, placeOfResidence: String, birthday: Date, sex : String) : SupporterStub =
    SupporterStub(Some(firstName), Some(lastName), Some(s"${firstName} ${lastName}"), Some(mobilePhone), Some(placeOfResidence), Some(birthday.getTime()), Some(sex), None, Set(), Set(), Set(), None, None)

  def apply(firstName: Option[String], lastName: Option[String], fullName: Option[String]) : SupporterStub =
    SupporterStub(firstName, lastName, fullName, None, None, None, None, None, Set(), Set(), Set(), None, None)

  def apply(tuple: (Option[String], Option[String], Option[String], Option[String], Option[String], Option[Long], Option[String], Option[CrewStub], Set[Role], Set[Pillar], Set[AddressStub], Option[String], Option[Long])) : SupporterStub =
    SupporterStub(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7, tuple._8, tuple._9, tuple._10, tuple._11, tuple._12, tuple._13)

  implicit val supporterWrites : OWrites[SupporterStub] = (
    (JsPath \ "firstName").writeNullable[String] and
      (JsPath \ "lastName").writeNullable[String] and
      (JsPath \ "fullName").writeNullable[String] and
      (JsPath \ "mobilePhone").writeNullable[String] and
      (JsPath \ "placeOfResidence").writeNullable[String] and
      (JsPath \ "birthday").writeNullable[Long] and
      (JsPath \ "sex").writeNullable[String] and
      (JsPath \ "crew").writeNullable[CrewStub] and
      (JsPath \ "roles").write[Set[Role]] and
      (JsPath \ "pillars").write[Set[Pillar]] and
      (JsPath \ "address").write[Set[AddressStub]] and
      (JsPath \ "active").writeNullable[String] and
      (JsPath \ "nvmDate").writeNullable[Long]
    )(unlift(SupporterStub.unapply))
  implicit val supporterReads : Reads[SupporterStub] = (
    (JsPath \ "firstName").readNullable[String] and
      (JsPath \ "lastName").readNullable[String] and
      (JsPath \ "fullName").readNullable[String] and
      (JsPath \ "mobilePhone").readNullable[String] and
      (JsPath \ "placeOfResidence").readNullable[String] and
      (JsPath \ "birthday").readNullable[Long] and
      (JsPath \ "sex").readNullable[String] and
      (JsPath \ "crew").readNullable[CrewStub] and
      (JsPath \ "roles").read[Set[Role]] and
      (JsPath \ "pillars").read[Set[Pillar]] and
      (JsPath \ "address").read[Set[AddressStub]] and
      (JsPath \ "active").readNullable[String] and
      (JsPath \ "nvmDate").readNullable[Long]
    ).tupled.map(SupporterStub( _ ))
}

case class ProfileStub(
                    loginInfo:LoginInfo,
                    confirmed: Boolean,
                    email:Option[String],
                    supporter: SupporterStub,
                    passwordInfo:Option[PasswordInfo],
                    oauth1Info: Option[OAuth1Info]) {
  def toProfile(c: Option[Crew], a: Set[Address]) : Profile =
    Profile(loginInfo, confirmed, email, supporter.toSupporter(c, a), passwordInfo, oauth1Info)
}

object ProfileStub {

  def apply(loginInfo : LoginInfo, email: String, firstName: String, lastName: String, mobilePhone: String, placeOfResidence: String, birthday: Date, sex : String) : ProfileStub =
  // confirmation is false by default, because this apply function is designed for using it during the default sign up process
    ProfileStub(loginInfo, false, Some(email), SupporterStub(firstName, lastName, mobilePhone, placeOfResidence, birthday, sex), None, None)

  def apply(tuple: (LoginInfo, Boolean, Option[String], SupporterStub, Option[PasswordInfo], Option[OAuth1Info])) : ProfileStub =
    ProfileStub(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6)

  def apply(p: CommonSocialProfile) : ProfileStub =
    ProfileStub(p.loginInfo, true, p.email, SupporterStub(p.firstName, p.lastName, p.fullName), None, None)

  implicit val passwordInfoJsonFormat = Json.format[PasswordInfo]
  implicit val oauth1InfoJsonFormat = Json.format[OAuth1Info]
  implicit val profileWrites : OWrites[ProfileStub] = (
    (JsPath \ "loginInfo").write[LoginInfo] and
      (JsPath \ "confirmed").write[Boolean] and
      (JsPath \ "email").writeNullable[String] and
      (JsPath \ "supporter").write[SupporterStub] and
      (JsPath \ "passwordInfo").writeNullable[PasswordInfo] and
      (JsPath \ "oauth1Info").writeNullable[OAuth1Info] 
    )(unlift(ProfileStub.unapply))
  implicit val profileReads : Reads[ProfileStub] = (
    (JsPath \ "loginInfo").read[LoginInfo] and
      (JsPath \ "confirmed").read[Boolean] and
      (JsPath \ "email").readNullable[String] and
      (JsPath \ "supporter").read[SupporterStub] and
      (JsPath \ "passwordInfo").readNullable[PasswordInfo] and
      (JsPath \ "oauth1Info").readNullable[OAuth1Info]
    ).tupled.map(ProfileStub( _ ))
}

case class UserStub(id: UUID, profiles: List[ProfileStub], updated : Long, created : Long, roles: Set[Role] = Set(RoleSupporter)) extends Identity {
  def toUser(p: List[Profile]) = User(id, p, updated, created, roles)
}
object UserStub {
  implicit val passwordInfoJsonFormat = Json.format[PasswordInfo]
  implicit val oauth1InfoJsonFormat = Json.format[OAuth1Info]
  implicit val userJsonFormat = Json.format[UserStub]
}
