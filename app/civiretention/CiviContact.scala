package civiretention

import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date

import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._

case class DoNot(email: Boolean, phone: Boolean, mail: Boolean, sms: Boolean, trade: Boolean)

object DoNot {
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
                        id : Long,
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
}

object CiviContact {
  def apply(
             id: String, contactType : String, displayName: String, do_not_email: String, do_not_phone: String, do_not_mail: String,
             do_not_sms: String, do_not_trade: String, image_url: String, firstName: String, lastName: String, nickname: String,
             gender: String, birth_date: String, place_of_residence: String, phone: String, email: String
           ): CiviContact =
    CiviContact(
      id.toLong,
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
}
