package civiretention

import models.User
import play.api.libs.json.{JsPath, OWrites, Reads}
import play.api.libs.functional.syntax._
import scala.language.implicitConversions

/**
  * Created by johann on 02.10.17.
  */
case class CiviPhone(
                    id : Option[Long],
                    contact_id : Option[Long],
                    location_type_id: Long,
                    is_primary : Boolean,
                    is_billing : Boolean,
                    phone : String,
                    phone_numeric: String,
                    phone_type_id: Long
                    ) {
  def setContact(contact_id: Long) : CiviPhone = this.copy(contact_id = Some(contact_id))
}
object CiviPhone {
  def apply(id : String, contact_id : String, location_type_id : String, is_primary: String, is_billing: String, phone: String,
            phone_numeric: String, phone_type_id : String) : CiviPhone =
    CiviPhone(
      Some(id.toLong), Some(contact_id.toLong), location_type_id.toLong,
      if(is_primary == "0") false else if(is_primary == "1") true else is_primary.toBoolean,
      if(is_billing == "0") false else if(is_billing == "1") true else is_billing.toBoolean,
      phone,
      phone_numeric,
      phone_type_id.toLong
    )

  def apply(t : (String, String, String, String, String, String, String, String)) : CiviPhone =
    CiviPhone(t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8)

  def apply(user: User) : List[CiviPhone] = {
    user.profiles.map((profile) =>
      profile.supporter.mobilePhone.map((phone) => CiviPhone(None, None, 1, profile == user.profiles.head, false, phone, phone.replaceAll("[^\\d.]", ""), 2))
    ).filter(_.isDefined).map(_.get)
  }

  implicit val civiPhoneReads: Reads[CiviPhone] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "contact_id").read[String] and
      (JsPath \ "location_type_id").read[String] and
      (JsPath \ "is_primary").read[String] and
      (JsPath \ "is_billing").read[String] and
      (JsPath \ "phone").read[String] and
      (JsPath \ "phone_numeric").read[String] and
      (JsPath \ "phone_type_id").read[String]
    ).tupled.map(CiviPhone(_))

  implicit val civiPhoneWrites : OWrites[CiviPhone] = (
    (JsPath \ "id").writeNullable[String] and
      (JsPath \ "contact_id").writeNullable[String] and
      (JsPath \ "location_type_id").write[String] and
      (JsPath \ "is_primary").write[String] and
      (JsPath \ "is_billing").write[String] and
      (JsPath \ "phone").write[String] and
      (JsPath \ "phone_numeric").write[String] and
      (JsPath \ "phone_type_id").write[String]
    )((phone) => (
      phone.id.map(_.toString),
      phone.contact_id.map(_.toString),
      phone.location_type_id.toString,
      phone.is_primary.toString,
      phone.is_billing.toString,
      phone.phone,
      phone.phone_numeric,
      phone.phone_type_id.toString
    ))

  implicit def civiQueryString(phone: CiviPhone) : Map[String, String] =
      phone.id.map((id) => Map("id" -> id.toString)).getOrElse(Map()) ++
      phone.contact_id.map((contact_id) => Map("contact_id" -> contact_id.toString)).getOrElse(Map()) ++
      Map(
        "location_type_id" -> phone.location_type_id.toString,
        "is_primary" -> phone.is_primary.toString,
        "is_billing" -> phone.is_billing.toString,
        "phone" -> phone.phone,
        "phone_numeric" -> phone.phone_numeric,
        "phone_type_id" -> phone.phone_type_id.toString
      )
}
