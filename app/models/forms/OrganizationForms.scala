package models.forms

import java.util.Date

import play.api.data.Form
import play.api.data.Forms._

import play.api.libs.concurrent.Execution.Implicits._

import play.api.i18n.{I18nSupport, Messages, MessagesApi}
object OrganizationForms {

  case class OrganizationFormData(
    name: String,
    address: String,
    telefon: String,
    fax: String,
    email: String,
    executive: String,
    abbreviation: String,
    impressum: String
  )
  
  val organizationForm = Form(mapping(
    "name" -> nonEmptyText,
    "address" -> nonEmptyText,
    "telefon" -> nonEmptyText,
    "fax" -> nonEmptyText,
    "email" -> nonEmptyText,
    "executive" -> nonEmptyText,
    "abbreviation" -> nonEmptyText,
    "impressum" -> nonEmptyText
  )(OrganizationFormData.apply)(OrganizationFormData.unapply))
}
  
  
