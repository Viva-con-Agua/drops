package civiretention

import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._

/**
  * Created by johann on 24.10.17.
  */
case class CiviAddress(
                      id: Int,
                      contact_id: Int,
                      location_type_id: Int,
                      is_primary : Boolean,
                      is_billing : Boolean,
                      streetAdress : String,
                      city : String,
                      postalCode: String,
                      country_id: Int
                     )
object CiviAddress {
  def apply(id : String, contact_id : String, location_type_id : String, is_primary: String, is_billing: String, streetAddress: String,
            city: String, postalCode: String, country_id: String) : CiviAddress =
    CiviAddress(
      id.toInt, contact_id.toInt, location_type_id.toInt,
      if(is_primary == "0") false else if(is_primary == "1") true else is_primary.toBoolean,
      if(is_billing == "0") false else if(is_billing == "1") true else is_billing.toBoolean,
      streetAddress, city, postalCode, country_id.toInt
    )

  def apply(t : (String, String, String, String, String, String, String, String, String)) : CiviAddress =
    CiviAddress(t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8, t._9)

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
}