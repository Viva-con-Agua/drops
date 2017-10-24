package civiretention

import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._

/**
  * Created by johann on 24.10.17.
  */
case class CiviEmail(
                    id: Int,
                    contact_id: Int,
                    location_type_id: Int,
                    is_primary : Boolean,
                    is_billing : Boolean,
                    address: String,
                    on_hold: Boolean,
                    is_bulkmail: Boolean
                    )
object CiviEmail {
  def apply(id : String, contact_id : String, location_type_id : String, is_primary: String, is_billing: String, address: String,
            on_hold: String, is_bulkmail: String) : CiviEmail =
    CiviEmail(
      id.toInt, contact_id.toInt, location_type_id.toInt,
      if(is_primary == "0") false else if(is_primary == "1") true else is_primary.toBoolean,
      if(is_billing == "0") false else if(is_billing == "1") true else is_billing.toBoolean,
      address,
      if(on_hold == "0") false else if(on_hold == "1") true else on_hold.toBoolean,
      if(is_bulkmail == "0") false else if(is_bulkmail == "1") true else is_bulkmail.toBoolean
    )

  def apply(t : (String, String, String, String, String, String, String, String)) : CiviEmail =
    CiviEmail(t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8)

  implicit val civiEmailReads: Reads[CiviEmail] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "contact_id").read[String] and
      (JsPath \ "location_type_id").read[String] and
      (JsPath \ "is_primary").read[String] and
      (JsPath \ "is_billing").read[String] and
      (JsPath \ "address").read[String] and
      (JsPath \ "on_hold").read[String] and
      (JsPath \ "is_bulkmail").read[String]
    ).tupled.map(CiviEmail(_))
}