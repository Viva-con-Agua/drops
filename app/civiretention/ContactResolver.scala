package civiretention

import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import javax.annotation.meta.TypeQualifierNickname
import javax.inject.Inject

import play.api.i18n.Messages
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}

import scala.concurrent.Future

/**
  * Created by johann on 15.09.17.
  */
class ContactResolver @Inject() (civi: CiviApi){

  def getAll(implicit messages: Messages) : Future[List[CiviContact]] = civi.get[CiviContact]("contact")//.map(_.map(
//    (contact) => // TODO!
//  ))
}