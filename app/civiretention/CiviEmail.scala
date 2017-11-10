package civiretention

import models.User
import play.api.libs.json.{JsPath, OWrites, Reads}
import play.api.libs.functional.syntax._
import scala.language.implicitConversions

/**
  * Created by johann on 24.10.17.
  */
case class CiviEmail(
                    id: Option[Long],
                    contact_id: Option[Long],
                    location_type_id: Long,
                    is_primary : Boolean,
                    is_billing : Boolean,
                    address: String,
                    on_hold: Boolean,
                    is_bulkmail: Boolean
                    ) {
  def setContact(contact_id: Long) : CiviEmail = this.copy(contact_id = Some(contact_id))
}
object CiviEmail {
  def apply(id : String, contact_id : String, location_type_id : String, is_primary: String, is_billing: String, address: String,
            on_hold: String, is_bulkmail: String) : CiviEmail =
    CiviEmail(
      Some(id.toLong), Some(contact_id.toLong), location_type_id.toLong,
      if(is_primary == "0") false else if(is_primary == "1") true else is_primary.toBoolean,
      if(is_billing == "0") false else if(is_billing == "1") true else is_billing.toBoolean,
      address,
      if(on_hold == "0") false else if(on_hold == "1") true else on_hold.toBoolean,
      if(is_bulkmail == "0") false else if(is_bulkmail == "1") true else is_bulkmail.toBoolean
    )

  def apply(t : (String, String, String, String, String, String, String, String)) : CiviEmail =
    CiviEmail(t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8)

  def apply(user: User) : List[CiviEmail] = {
    user.profiles.map((profile) =>
      profile.email.map((email) => CiviEmail(None, None, 1, profile == user.profiles.head, false, email, profile.confirmed, false))
    ).filter(_.isDefined).map(_.get)
  }

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


  implicit val civiEmailWrites : OWrites[CiviEmail] = (
    (JsPath \ "id").writeNullable[String] and
      (JsPath \ "contact_id").writeNullable[String] and
      (JsPath \ "location_type_id").write[String] and
      (JsPath \ "is_primary").write[String] and
      (JsPath \ "is_billing").write[String] and
      (JsPath \ "address").write[String] and
      (JsPath \ "on_hold").write[String] and
      (JsPath \ "is_bulkmail").write[String]
    )((email) => (
      email.id.map(_.toString),
      email.contact_id.map(_.toString),
      email.location_type_id.toString,
      email.is_primary.toString,
      email.is_billing.toString,
      email.address,
      email.on_hold.toString,
      email.is_bulkmail.toString
    ))

  implicit def civiQueryString(email: CiviEmail) : Map[String, String] =
      email.id.map((id) => Map("id" -> id.toString)).getOrElse(Map()) ++
      email.contact_id.map((contact_id) => Map("contact_id" -> contact_id.toString)).getOrElse(Map()) ++
      Map(
        "location_type_id" -> email.location_type_id.toString,
        "is_primary" -> email.is_primary.toString,
        "is_billing" -> email.is_billing.toString,
        "address" -> email.address,
        "on_hold" -> email.on_hold.toString,
        "is_bulkmail" -> email.is_bulkmail.toString
      )
}