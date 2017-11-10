package civiretention

import java.net.URI
import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import models.User
import play.api.libs.json.{JsPath, OWrites, Reads}
import play.api.libs.functional.syntax._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

case class DoNot(email: Boolean, phone: Boolean, mail: Boolean, sms: Boolean, trade: Boolean)

object DoNot {
  val DefaultSupporter = DoNot(false, false, false, false, true)

  def apply(email: String, phone: String, mail: String, sms: String, trade: String) : DoNot = {
    val emailBool = if(email == "0") false else if(email == "1") true else email.toBoolean
    val phoneBool = if(phone == "0") false else if(phone == "1") true else phone.toBoolean
    val mailBool = if(mail == "0") false else if(mail == "1") true else mail.toBoolean
    val smsBool = if(sms == "0") false else if(sms == "1") true else sms.toBoolean
    val tradeBool = if(trade == "0") false else if(trade == "1") true else trade.toBoolean
    DoNot(emailBool, phoneBool, mailBool, smsBool, tradeBool)
  }
}

trait Gender {
  val stringify : String
}
object Gender {
  def apply(gender: String) : Gender =
    gender match {
      case "male" => MaleGender
      case "female" => FemaleGender
      case "other" => OtherGender
      case "männlich" => MaleGender
      case "weiblich" => FemaleGender
      case "unbestimmt" => OtherGender
      case _ => OtherGender
    }
}
object MaleGender extends Gender {
  override val stringify = "male"
}
object FemaleGender extends Gender {
  override val stringify = "female"
}
object OtherGender extends Gender {
  override val stringify = "other"
}

trait ContactType {
  val name : String
}
object ContactType {
  def apply(contactType: String) : ContactType = contactType match {
    case Individual.name => Individual
    case Organization.name => Organization
    case Household.name => Household
    case _ => throw ContactTypeException("Given contact type not found!") // Todo: messages with parameter!
  }
}
object Individual extends ContactType {
  override val name: String = "Individual"
}
object Organization extends ContactType {
  override val name: String = "Organization"
}
object Household extends ContactType {
  override val name: String = "Household"
}

case class CiviContact(
                        id : Option[Long],
                        contactType: ContactType,
                        displayName: String,
                        do_not: DoNot,
                        image_url : Option[URI],
                        firstName : String,
                        lastName : String,
                        nickname: String,
                        gender: Option[Gender],
                        birth_date: Option[Date],
                        place_of_residence: String,
                        phone: String,
                        email: String
                      ) {
  override def toString: String = this.displayName + " (" + this.email + ")"

  def birthString(dateFormat : Option[SimpleDateFormat] = None): Option[String] = this.birth_date match {
    case Some(date) => {
      val format = dateFormat.getOrElse(new SimpleDateFormat("yyyy-mm-dd"))
      Some(format.format(date))
    }
    case _ => None
  }
}

object CiviContact {
  def apply(
             id: String, contactType : String, displayName: String, do_not_email: String, do_not_phone: String, do_not_mail: String,
             do_not_sms: String, do_not_trade: String, image_url: String, firstName: String, lastName: String, nickname: String,
             gender: String, birth_date: String, place_of_residence: String, phone: String, email: String
           ): CiviContact =
    CiviContact(
      Some(id.toLong),
      ContactType(contactType),
      displayName,
      DoNot(do_not_email, do_not_phone, do_not_mail, do_not_sms, do_not_trade),
      if(image_url != "") Some(new URI(image_url)) else None,
      firstName,
      lastName,
      nickname,
      if(gender != "") Some(Gender(gender)) else None,
      if(birth_date != "") {
        val df = new SimpleDateFormat("yyyy-mm-dd")
        Some(df.parse(birth_date))
      } else None,
      place_of_residence,
      phone,
      email
    )

  def apply(t: Tuple17[String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String]): CiviContact = {
    CiviContact(t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8, t._9, t._10, t._11, t._12, t._13, t._14, t._15, t._16, t._17)
  }

  def apply(user: User) : Future[Option[CiviContact]] = user.profiles.headOption.map((profile) =>
    profile.avatar.headOption.map(_.getImage(400,400)).getOrElse(Future.successful(None)).map(_.map((image) =>
      CiviContact(
        None,
        Individual,
        profile.supporter.fullName.getOrElse(""),
        DoNot.DefaultSupporter,
        Some(URI.create( image )),
        profile.supporter.firstName.getOrElse(""),
        profile.supporter.lastName.getOrElse(""),
        profile.supporter.username.getOrElse(""),
        profile.supporter.sex.map(Gender( _ )),
        profile.supporter.birthDate,
        profile.supporter.placeOfResidence.getOrElse(""),
        profile.supporter.mobilePhone.getOrElse(""),
        profile.email.getOrElse("")
      )
    ))
  ).getOrElse(Future.successful(None))

  implicit val civiUserReads: Reads[CiviContact] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "contact_type").read[String] and
      (JsPath \ "display_name").read[String] and
      (JsPath \ "do_not_email").read[String] and
      (JsPath \ "do_not_phone").read[String] and
      (JsPath \ "do_not_mail").read[String] and
      (JsPath \ "do_not_sms").read[String] and
      (JsPath \ "do_not_trade").read[String] and
      (JsPath \ "image_URL").read[String] and
      (JsPath \ "first_name").read[String] and
      (JsPath \ "last_name").read[String] and
      (JsPath \ "nick_name").read[String] and
      (JsPath \ "gender").read[String] and
      (JsPath \ "birth_date").read[String] and
      (JsPath \ "city").read[String] and
      (JsPath \ "phone").read[String] and
      (JsPath \ "email").read[String]
    ).tupled.map(CiviContact(_))

  implicit val civiUserWrites : OWrites[CiviContact] = (
    (JsPath \ "id").writeNullable[String] and
      (JsPath \ "contact_type").write[String] and
      (JsPath \ "display_name").write[String] and
      (JsPath \ "do_not_email").write[String] and
      (JsPath \ "do_not_phone").write[String] and
      (JsPath \ "do_not_mail").write[String] and
      (JsPath \ "do_not_sms").write[String] and
      (JsPath \ "do_not_trade").write[String] and
      (JsPath \ "image_URL").write[String] and
      (JsPath \ "first_name").write[String] and
      (JsPath \ "last_name").write[String] and
      (JsPath \ "nick_name").write[String] and
      (JsPath \ "gender").write[String] and
      (JsPath \ "birth_date").write[String] and
      (JsPath \ "city").write[String] and
      (JsPath \ "phone").write[String] and
      (JsPath \ "email").write[String]
    )((contact) => (
      contact.id.map(_.toString),
      contact.contactType.name,
      contact.displayName,
      contact.do_not.email.toString,
      contact.do_not.phone.toString,
      contact.do_not.mail.toString,
      contact.do_not.sms.toString,
      contact.do_not.trade.toString,
      contact.image_url.filter(_.isAbsolute).map(_.toASCIIString).getOrElse(""),
      contact.firstName,
      contact.lastName,
      contact.nickname,
      contact.gender.map(_.stringify).getOrElse(OtherGender.stringify),
      contact.birthString().getOrElse(""),
      contact.place_of_residence,
      contact.phone,
      contact.email
    ))

  implicit def civiQueryString(contact: CiviContact) : Map[String, String] =
    contact.id.map((id) => Map("id" -> id.toString)).getOrElse(Map()) ++
    contact.image_url.filter(_.isAbsolute).map((url) => Map("image_URL" -> url.toASCIIString)).getOrElse(Map()) ++
    Map(
      "contact_type" -> contact.contactType.name,
      "display_name" -> contact.displayName,
      "do_not_email" -> contact.do_not.email.toString,
      "do_not_phone" -> contact.do_not.phone.toString,
      "do_not_mail" -> contact.do_not.mail.toString,
      "do_not_sms" -> contact.do_not.sms.toString,
      "do_not_trade" -> contact.do_not.trade.toString,
      "first_name" -> contact.firstName,
      "last_name" -> contact.lastName,
      "nick_name" -> contact.nickname,
      "gender" -> contact.gender.map(_.stringify).getOrElse(OtherGender.stringify),
      "birth_date" -> contact.birthString().getOrElse(""),
      "city" -> contact.place_of_residence,
      "phone" -> contact.phone,
      "email" -> contact.email
    )
}
