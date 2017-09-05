package controllers

import java.util.Date

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
//import play.api.i18n.Messages
//import play.api.Play.current
//import play.api.i18n.Messages.Implicits._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits._

/**
  * Created by johann on 04.09.17.
  */
object UserForms {

  object UserConstraints {
    val allNumbers = """\d{4}[-/\s]?\d*""".r//"""(\d{4})-(\d*)""".r
    val sex = """male|female|other""".r

    def telephoneCheck(implicit messages:Messages): Constraint[String] = Constraint("constraints.telephone")(
      _ match {
        case allNumbers() => Valid
        case _ => Invalid(Seq(ValidationError(Messages("error.telphone.wrongFormat"))))
      }
    )
    def sexCheck(implicit messages:Messages): Constraint[String] = Constraint("constraints.sex")(
      _ match {
        case sex() => Valid
        case _ => Invalid(Seq(ValidationError(Messages("error.sex.notAllowed"))))
      }
    )
  }

  case class UserData(
                       email: String,
                       firstName : String,
                       lastName: String,
                       mobilePhone: String,
                       placeOfResidence: String,
                       birthday:Date,
                       sex:String
                     )

  def userForm(implicit messages:Messages) = Form(mapping(
    "email" -> email,
    "firstName" -> nonEmptyText,
    "lastName" -> nonEmptyText,
    "mobilePhone" -> nonEmptyText(minLength = 5).verifying(UserConstraints.telephoneCheck),
    "placeOfResidence" -> nonEmptyText,
    "birthday" -> date,
    "sex" -> nonEmptyText.verifying(UserConstraints.sexCheck)
  )(UserData.apply)(UserData.unapply))
}
