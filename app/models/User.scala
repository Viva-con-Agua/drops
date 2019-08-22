package models

import java.net.URL
import java.text.SimpleDateFormat
import java.util.{Calendar, Date, UUID}

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, OWrites, Reads}
import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.providers.OAuth1Info
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import play.api.i18n.Messages

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

case class Supporter(
  firstName: Option[String],
  lastName: Option[String],
  fullName: Option[String],
  mobilePhone: Option[String],
  placeOfResidence: Option[String],
  birthday: Option[Long],
  sex: Option[String],
  crew: Option[Crew],
  roles: Set[Role],
  pillars: Set[Pillar],
  address: Set[Address],
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

  def name : Option[String] = this.firstName.flatMap(fn => lastName.map(fn + " " + _))

  def toSupporterStub() : SupporterStub =
    SupporterStub(firstName, lastName, fullName, mobilePhone, placeOfResidence, birthday, sex, (if(crew.isDefined) Option(crew.get.toCrewStub()) else None), roles, pillars, address.map(_.toAddressStub), active, nvmDate)
}

object Supporter {

  def apply(firstName: Option[String], lastName: Option[String], fullName: Option[String], mobilePhone: Option[String], placeOfResidence: Option[String], birthday: Option[Long], sex: Option[String]): Supporter = {
    Supporter(firstName, lastName, fullName, mobilePhone, placeOfResidence, birthday, sex, None, Set(), Set(), Set(), None, None)
  }

  def apply(firstName: Option[String], lastName: Option[String], mobilePhone: Option[String], placeOfResidence: Option[String], birthday: Option[Long], sex: Option[String]): Supporter = {
    Supporter(firstName, lastName, None, mobilePhone, placeOfResidence, birthday, sex, None, Set(), Set(), Set(), None, None)
  }

  def apply(firstName: Option[String], lastName: Option[String], fullName: Option[String], mobilePhone: Option[String], placeOfResidence: Option[String], birthday: Option[Long], sex: Option[String], crew: Option[Crew], roles: Set[Role], address: Set[Address]): Supporter = {
    Supporter(firstName, lastName, fullName, mobilePhone, placeOfResidence, birthday, sex, crew, roles, Set(), address, None, None)
  }

  def apply(firstName: Option[String], lastName: Option[String], mobilePhone: Option[String], placeOfResidence: Option[String], birthday: Option[Long], sex: Option[String], crew: Option[Crew], roles: Set[Role]): Supporter = {
    Supporter(firstName, lastName, None, mobilePhone, placeOfResidence, birthday, sex, crew, roles, Set(), Set(), None, None)
  }

  def apply(firstName: String, lastName: String, mobilePhone: String, placeOfResidence: String, birthday: Long, sex: String): Supporter =
    Supporter(Some(firstName), Some(lastName), Some(s"${firstName} ${lastName}"), Some(mobilePhone), Some(placeOfResidence), Some(birthday), Some(sex), None, Set(), Set(), Set(), None, None)

  def apply(firstName: String, lastName: String, mobilePhone: String, placeOfResidence: String, birthday: Date, sex : String) : Supporter =
    Supporter(Some(firstName), Some(lastName), Some(s"${firstName} ${lastName}"), Some(mobilePhone), Some(placeOfResidence), Some(birthday.getTime()), Some(sex), None, Set(), Set(), Set(), None, None)

  def apply(firstName: Option[String], lastName: Option[String], mobilePhone: Option[String], placeOfResidence: Option[String], birthday: Option[Date], sex : String, address: Option[AddressStub]) : Supporter =
    Supporter(firstName, lastName, firstName.flatMap(fn => lastName.map(ln => s"${fn} ${ln}")), mobilePhone, placeOfResidence, birthday.map(_.getTime()), Some(sex), None, Set(), Set(), if(address != None) Set(address.head.toAddress) else Set(), None, None)

  def apply(firstName: Option[String], lastName: Option[String], fullName: Option[String]) : Supporter =
    Supporter(firstName, lastName, fullName, None, None, None, None, None, Set(), Set(), Set(), None, None)

  def apply(firstName: Option[String], lastName: Option[String], fullName: Option[String], mobilePhone: Option[String], placeOfResidence: Option[String], birthday : Option[Long], sex : Option[String], crew: Option[Crew], roles: Set[Role], address : Set[Address], active : Option[String], nvmDate : Option[Long]) : Supporter =
    Supporter(firstName, lastName, fullName, mobilePhone, placeOfResidence, birthday, sex, crew, roles, Set(), address, active, nvmDate)

  def apply(tuple: (Option[String],  Option[String], Option[String], Option[String], Option[String], Option[Long], Option[String], Option[Crew], Set[Role], Set[Pillar], Set[Address], Option[String], Option[Long])) : Supporter =
    Supporter(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7, tuple._8, tuple._9, tuple._10, tuple._11, tuple._12, tuple._13)
  
  implicit val supporterWrites : OWrites[Supporter] = (
    (JsPath \ "firstName").writeNullable[String] and
      (JsPath \ "lastName").writeNullable[String] and
      (JsPath \ "fullName").writeNullable[String] and
      (JsPath \ "mobilePhone").writeNullable[String] and
      (JsPath \ "placeOfResidence").writeNullable[String] and
      (JsPath \ "birthday").writeNullable[Long] and
      (JsPath \ "sex").writeNullable[String] and
      (JsPath \ "crew").writeNullable[Crew] and
      (JsPath \ "roles").write[Set[Role]] and
      (JsPath \ "pillars").write[Set[Pillar]] and
      (JsPath \ "address").write[Set[Address]] and 
      (JsPath \ "active").writeNullable[String] and
      (JsPath \ "nvmDate").writeNullable[Long]
    )(unlift(Supporter.unapply))
  implicit val supporterReads : Reads[Supporter] = (
      (JsPath \ "firstName").readNullable[String] and
      (JsPath \ "lastName").readNullable[String] and
      (JsPath \ "fullName").readNullable[String] and
      (JsPath \ "mobilePhone").readNullable[String] and
      (JsPath \ "placeOfResidence").readNullable[String] and
      (JsPath \ "birthday").readNullable[Long] and
      (JsPath \ "sex").readNullable[String] and
      (JsPath \ "crew").readNullable[Crew] and
      (JsPath \ "roles").read[Set[Role]] and
      (JsPath \ "pillars").read[Set[Pillar]] and
      (JsPath \ "address").read[Set[Address]] and
      (JsPath \ "active").readNullable[String] and
      (JsPath \ "nvmDate").readNullable[Long]
    ).tupled.map(Supporter( _ ))
}

case class Profile(
  loginInfo:LoginInfo,
  confirmed: Boolean,
  email:Option[String],
  supporter: Supporter,
  passwordInfo:Option[PasswordInfo], 
  oauth1Info: Option[OAuth1Info]) {

  def toProfileStub : ProfileStub =
    ProfileStub(loginInfo, confirmed, email, supporter.toSupporterStub(), passwordInfo, oauth1Info)
}

object Profile {

  def apply(loginInfo: LoginInfo, confirmed: Boolean, email: String, supporter: Supporter, passwordInfo: Option[PasswordInfo], oauth1Info: Option[OAuth1Info]) : Profile =
    Profile(loginInfo, confirmed, Some(email), supporter, passwordInfo, oauth1Info)
  def apply(loginInfo: LoginInfo, confirmed: Boolean, email: String, firstName: String, lastName: String, mobilePhone: String, placeOfResidence: String, birthday: Long, sex: String, passwordInfo: Option[PasswordInfo]) :Profile =
    Profile(loginInfo, confirmed, Some(email), Supporter(firstName, lastName, mobilePhone, placeOfResidence, birthday, sex), passwordInfo, None)

  def apply(loginInfo: LoginInfo, email: String, firstName: String, lastName: String, mobilePhone: String, placeOfResidence: String, birthday: Long, sex: String) : Profile =
    Profile(loginInfo, false, Some(email), Supporter(firstName, lastName, mobilePhone, placeOfResidence, birthday, sex), None, None)

  def apply(loginInfo: LoginInfo, email: String, firstName: String, lastName: String, mobilePhone: String, placeOfResidence: String, birthday: Date, sex : String) : Profile =
    // confirmation is false by default, because this apply function is designed for using it during the default sign up process
    Profile(loginInfo, false, Some(email), Supporter(firstName, lastName, mobilePhone, placeOfResidence, birthday, sex), None, None)

  def apply(loginInfo: LoginInfo, email: String, firstName: Option[String], lastName: Option[String], mobilePhone:Option[String], placeOfResidence: Option[String], birthday:Option[Date], gender: String, address: Option[AddressStub]): Profile =

    Profile(loginInfo, false, Some(email), Supporter(firstName, lastName, mobilePhone, placeOfResidence, birthday, gender, address), None, None)

  def apply(tuple: (LoginInfo, Boolean, Option[String], Supporter, Option[PasswordInfo], Option[OAuth1Info])) : Profile =
    Profile(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6)

  def apply(p: CommonSocialProfile) : Profile =
    Profile(p.loginInfo, true, p.email, Supporter(p.firstName, p.lastName, p.fullName), None, None)

  implicit val passwordInfoJsonFormat = Json.format[PasswordInfo]
  implicit val oauth1InfoJsonFormat = Json.format[OAuth1Info]
  implicit val profileWrites : OWrites[Profile] = (
    (JsPath \ "loginInfo").write[LoginInfo] and
      (JsPath \ "confirmed").write[Boolean] and
      (JsPath \ "email").writeNullable[String] and
      (JsPath \ "supporter").write[Supporter] and
      (JsPath \ "passwordInfo").writeNullable[PasswordInfo] and
      (JsPath \ "oauth1Info").writeNullable[OAuth1Info] 
    )(unlift(Profile.unapply))
  implicit val profileReads : Reads[Profile] = (
      (JsPath \ "loginInfo").read[LoginInfo] and
      (JsPath \ "confirmed").read[Boolean] and
      (JsPath \ "email").readNullable[String] and
      (JsPath \ "supporter").read[Supporter] and
      (JsPath \ "passwordInfo").readNullable[PasswordInfo] and
      (JsPath \ "oauth1Info").readNullable[OAuth1Info] 
    ).tupled.map(Profile( _ ))
}

case class PublicProfile(
  loginInfo:LoginInfo,
  primary: Boolean,
  confirmed: Boolean,
  email:Option[String],
  supporter: Supporter)

object PublicProfile {
  def apply(profile: Profile, primary: Boolean = false) : PublicProfile =
      PublicProfile(profile.loginInfo, primary, profile.confirmed, profile.email, profile.supporter)

  def apply(tuple : (LoginInfo, Boolean, Boolean, Option[String], Supporter)) : PublicProfile =
    PublicProfile(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5)

  implicit val publicProfileWrites : OWrites[PublicProfile] = (
    (JsPath \ "loginInfo").write[LoginInfo] and
      (JsPath \ "primary").write[Boolean] and
      (JsPath \ "confirmed").write[Boolean] and
      (JsPath \ "email").writeNullable[String] and
      (JsPath \ "supporter").write[Supporter] 
    )(unlift(PublicProfile.unapply))
  implicit val publicProfileReads : Reads[PublicProfile] = (
    (JsPath \ "loginInfo").read[LoginInfo] and
      (JsPath \ "primary").read[Boolean] and
      (JsPath \ "confirmed").read[Boolean] and
      (JsPath \ "email").readNullable[String] and
      (JsPath \ "supporter").read[Supporter] 
    ).tupled.map(PublicProfile( _ ))
}

case class User(id: UUID, profiles: List[Profile], updated: Long, created: Long, roles: Set[Role] = Set(RoleSupporter)) extends Identity {
  def updateProfile(updatedProfile: Profile) = User(this.id, profiles.map(p => p.loginInfo match {
    case updatedProfile.loginInfo => updatedProfile
    case _ => p
  }), this.updated, this.created, this.roles)
  def profileFor(loginInfo:LoginInfo) = profiles.find(_.loginInfo == loginInfo)
  def fullName(loginInfo:LoginInfo) = profileFor(loginInfo).flatMap(_.supporter.fullName)
  def setRoles(roles : Set[Role]) = this.copy(roles = roles)
  def hasRole(role: Role) = this.roles.contains(role)

  def toUserStub : UserStub = {
    val profileStubList : List[ProfileStub] = List[ProfileStub]()
    profiles.foreach(profile => {
      profileStubList ++ List(profile.toProfileStub)
    })
    UserStub(id, profileStubList, updated, created, roles)
  }

}

object User {
  implicit val passwordInfoJsonFormat = Json.format[PasswordInfo]
  implicit val oauth1InfoJsonFormat = Json.format[OAuth1Info]
  implicit val userJsonFormat = Json.format[User]
}

case class PublicUser(id: UUID, profiles : List[PublicProfile], roles: Set[Role], updated: Long, created: Long)

object PublicUser {
  def apply(user : User) : PublicUser = PublicUser(
    user.id,
    // select the first profile as the primary profile:
    user.profiles.headOption.map(PublicProfile(_, true)).toList ++ user.profiles.tail.map(PublicProfile(_)),
    user.roles,
    user.updated,
    user.created
  )
  def apply(tuple : (UUID, List[PublicProfile], Set[Role], Long, Long)) : PublicUser = PublicUser(
    tuple._1, tuple._2, tuple._3, tuple._4, tuple._5
  )

  implicit val publicUserWrites : OWrites[PublicUser] = (
    (JsPath \ "id").write[UUID] and
      (JsPath \ "profiles").write[List[PublicProfile]] and
      (JsPath \ "roles").write[Set[Role]] and
      (JsPath \ "updated").write[Long] and
      (JsPath \ "created").write[Long]
    )(unlift(PublicUser.unapply))
  implicit val publicUserReads : Reads[PublicUser] = (
    (JsPath \ "id").read[UUID] and
      (JsPath \ "profiles").read[List[PublicProfile]] and
      (JsPath \ "roles").read[Set[Role]] and
      (JsPath \ "updated").read[Long] and
      (JsPath \ "created").read[Long]
    ).tupled.map(PublicUser( _ ))
}
