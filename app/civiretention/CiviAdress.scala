package civiretention

import models.User
import play.api.libs.json.{JsPath, OWrites, Reads}
import play.api.libs.functional.syntax._

import scala.language.implicitConversions

/**
  * Created by johann on 24.10.17.
  */
case class CiviAddress(
                      id: Option[Long],
                      contact_id: Option[Long],
                      location_type_id: Long,
                      is_primary : Boolean,
                      is_billing : Boolean,
                      streetAddress : String,
                      city : String,
                      postalCode: String,
                      country_id: Long
                     ) {
  def setContact(contact_id: Long) : CiviAddress = this.copy(contact_id = Some(contact_id))
}
object CiviAddress {
  def apply(id : String, contact_id : String, location_type_id : String, is_primary: String, is_billing: String, streetAddress: String,
            city: String, postalCode: String, country_id: String) : CiviAddress =
    CiviAddress(
      Some(id.toLong), Some(contact_id.toLong), location_type_id.toLong,
      if(is_primary == "0") false else if(is_primary == "1") true else is_primary.toBoolean,
      if(is_billing == "0") false else if(is_billing == "1") true else is_billing.toBoolean,
      streetAddress, city, postalCode, country_id.toLong
    )

  def apply(t : (String, String, String, String, String, String, String, String, String)) : CiviAddress =
    CiviAddress(t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8, t._9)

  def apply(user: User) : List[CiviAddress] = {
    user.profiles.map((profile) =>
      profile.supporter.placeOfResidence.map((city) => CiviAddress(None, None, 1, profile == user.profiles.head, false, "", city, "", 1082))
    ).filter(_.isDefined).map(_.get)
  }

  implicit val civiAddressReads: Reads[CiviAddress] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "contact_id").read[String] and
      (JsPath \ "location_type_id").read[String] and
      (JsPath \ "is_primary").read[String] and
      (JsPath \ "is_billing").read[String] and
      (JsPath \ "streetAddress").read[String] and
      (JsPath \ "city").read[String] and
      (JsPath \ "postalCode").read[String] and
      (JsPath \ "country_id").read[String]
    ).tupled.map(CiviAddress(_))

  implicit val civiAddressWrites : OWrites[CiviAddress] = (
    (JsPath \ "id").writeNullable[String] and
      (JsPath \ "contact_id").writeNullable[String] and
      (JsPath \ "location_type_id").write[String] and
      (JsPath \ "is_primary").write[String] and
      (JsPath \ "is_billing").write[String] and
      (JsPath \ "streetAddress").write[String] and
      (JsPath \ "city").write[String] and
      (JsPath \ "postalCode").write[String] and
      (JsPath \ "country_id").write[String]
    )((address) => (
      address.id.map(_.toString),
      address.contact_id.map(_.toString),
      address.location_type_id.toString,
      address.is_primary.toString,
      address.is_billing.toString,
      address.streetAddress,
      address.city,
      address.postalCode,
      address.country_id.toString
    ))

  implicit def civiQueryString(address: CiviAddress) : Map[String, String] =
      address.id.map((id) => Map("id" -> id.toString)).getOrElse(Map()) ++
      address.contact_id.map((contact_id) => Map("contact_id" -> contact_id.toString)).getOrElse(Map()) ++
      Map(
        "location_type_id" -> address.location_type_id.toString,
        "is_primary" -> address.is_primary.toString,
        "is_billing" -> address.is_billing.toString,
        "streetAddress" -> address.streetAddress,
        "city" -> address.city,
        "postalCode" -> address.postalCode,
        "country_id" -> address.country_id.toString
      )
}