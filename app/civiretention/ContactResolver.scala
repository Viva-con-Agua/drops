package civiretention

import java.net.URI
import javax.inject.Inject

import play.api.i18n.Messages
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}

import scala.concurrent.Future

/**
  * Created by johann on 15.09.17.
  */
class ContactResolver @Inject() (civi: CiviApi){

  def getAll(implicit messages: Messages) : Future[List[CiviContact]] = civi.get[CiviContact]("contact")
}

case class DoNot(email: Boolean, phone: Boolean, mail: Boolean, sms: Boolean, trade: Boolean)
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
  override val name: String = "individual"
}
object Organization extends ContactType {
  override val name: String = "organization"
}
object Household extends ContactType {
  override val name: String = "household"
}

case class CiviContact(
                        id : Long,
                        contactType: ContactType,
                        displayName: String,
                        do_not: DoNot,
                        image_url : URI,
                        firstName : String,
                        lastName : String,
                        gender: String,
                        birth_date: String,
                        place_of_residence: String,
                        phone: String,
                        email: String
                      ) {
  override def toString: String = this.displayName + " (" + this.email + ")"
}

object CiviContact {
  def apply(
             id: String, contactType : String, displayName: String, do_not_email: String, do_not_phone: String, do_not_mail: String,
             do_not_sms: String, do_not_trade: String, image_url: String, firstName: String, lastName: String,
             gender: String, birth_date: String, place_of_residence: String, phone: String, email: String
           ): CiviContact =
    CiviContact(
      id.toLong,
      ContactType(contactType),
      displayName,
      DoNot(do_not_email.toBoolean, do_not_phone.toBoolean, do_not_mail.toBoolean, do_not_sms.toBoolean, do_not_trade.toBoolean),
      new URI(image_url),
      firstName,
      lastName,
      gender,
      birth_date,
      place_of_residence,
      phone,
      email
    )

  def apply(t: Tuple16[String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String]): CiviContact = {
    CiviContact(t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8, t._9, t._10, t._11, t._12, t._13, t._14, t._15, t._16)
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
      (JsPath \ "image_url").read[String] and
      (JsPath \ "first_name").read[String] and
      (JsPath \ "last_name").read[String] and
      (JsPath \ "gender").read[String] and
      (JsPath \ "birth_date").read[String] and
      (JsPath \ "city").read[String] and
      (JsPath \ "phone").read[String] and
      (JsPath \ "email").read[String]
    ).tupled.map(CiviContact(_))
}
