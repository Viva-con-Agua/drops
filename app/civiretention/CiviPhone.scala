package civiretention

import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._

/**
  * Created by johann on 02.10.17.
  */
case class CiviPhone(
                    id : Int,
                    contact_id : Int,
                    location_type_id: Int,
                    is_primary : Boolean,
                    is_billing : Boolean,
                    phone : String,
                    phone_numeric: String,
                    phone_type_id: Int
                    )
object CiviPhone {
  def apply(id : String, contact_id : String, location_type_id : String, is_primary: String, is_billing: String, phone: String,
            phone_numeric: String, phone_type_id : String) : CiviPhone =
    CiviPhone(
      id.toInt, contact_id.toInt, location_type_id.toInt,
      if(is_primary == "0") false else if(is_primary == "1") true else is_primary.toBoolean,
      if(is_billing == "0") false else if(is_billing == "1") true else is_billing.toBoolean,
      phone,
      phone_numeric,
      phone_type_id.toInt
    )

  def apply(t : (String, String, String, String, String, String, String, String)) : CiviPhone =
    CiviPhone(t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8)

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
}
