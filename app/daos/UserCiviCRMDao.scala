package daos

import java.net.URI
import javax.inject.Inject

import civiretention.{CiviContact, ContactResolver, Individual}
import models.User
import play.api.i18n.Messages
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by johann on 15.09.17.
  */
class UserCiviCRMDao @Inject() (contactResolver: ContactResolver, userDao: UserDao){
  def getAll(implicit messages: Messages) : Future[List[User]] = contactResolver.getAll.flatMap(
    (list) => Future.sequence(list.map(mapper( _ )))
  ).map(_.filter(_.isDefined).map(_.get))

  private def mapper(contact: CiviContact) : Future[Option[User]] = contact.contactType match {
    case Individual => userDao.find(contact.email)
    case _ => Future.successful(None)
  }
}