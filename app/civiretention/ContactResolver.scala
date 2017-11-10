package civiretention

import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import javax.annotation.meta.TypeQualifierNickname
import javax.inject.Inject

import play.api.http.Writeable
import play.api.i18n.Messages
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by johann on 15.09.17.
  */
class ContactResolver @Inject() (civi: CiviApi){

  def getAll(implicit messages: Messages) : Future[List[CiviContactContainer]] = civi.get[CiviContact]("contact").flatMap((l) => Future.sequence(l.map(
    (contact) => {
      val phones = civi.get[CiviPhone]("phone", Map("contact_id" -> contact.id.toString))
      val emails = civi.get[CiviEmail]("email", Map("contact_id" -> contact.id.toString))
      val addresses = civi.get[CiviAddress]("address", Map("contact_id" -> contact.id.toString))
      phones.flatMap((phones) => emails.flatMap((emails) => addresses.map(
        CiviContactContainer(contact, phones, emails, _)
      )))
    }
  )))

  def create(user: CiviContactContainer)(implicit messages: Messages) : Future[List[CiviContactContainer]] = {
    civi.create[CiviContact](user.contact, "contact").flatMap((l) => Future.sequence(l.map(
      (contact) => {
        val phones : Future[List[CiviPhone]] = Future.sequence(
          user.phones.map((phone) => civi.create[CiviPhone](phone, "phone"))
        ).map((f) => f.flatten)
        val emails : Future[List[CiviEmail]] = Future.sequence(
          user.emails.map((email) => civi.create[CiviEmail](email, "email"))
        ).map((f) => f.flatten)
        val addresses : Future[List[CiviAddress]] = Future.sequence(
          user.addresses.map((address) => civi.create[CiviAddress](address, "phone"))
        ).map((f) => f.flatten)

        for {
          phonesList <- phones
          emailsList <- emails
          addressList <- addresses
        } yield CiviContactContainer(contact, phonesList, emailsList, addressList)
      }
    )))
  }


  implicit def jsonWriteable[T](implicit writes: Writes[T]): Writeable[T] = {
    val jsonWriteable = implicitly[Writeable[JsValue]]
    def transform(obj: T) = jsonWriteable.transform(Json.toJson(obj))
    new Writeable[T](transform, jsonWriteable.contentType)
  }
}