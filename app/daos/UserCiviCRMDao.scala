package daos

import java.net.URI
import java.util.UUID
import javax.inject.Inject

import civiretention.{CiviContact, ContactResolver, Individual}
import com.mohiva.play.silhouette.api.LoginInfo
import models.{CiviProfileImage, Profile, Supporter, User}
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
    (list) => Future.sequence(list.map(findOrSave( _ )))
  ).map(_.filter(_.isDefined).map(_.get))

  private def mapper(contact: CiviContact)(success: Option[User] => Future[Option[User]], failure: () => Future[Option[User]]) : Future[Option[User]] = contact.contactType match {
    case Individual => userDao.find(contact.email).flatMap((res) => res.isDefined match {
      case true => success(res)
      case false => failure()
    } )
    case _ => Future.successful(None)
  }

  private def findOrSave(contact: CiviContact) : Future[Option[User]] = mapper(contact)(
    (userOpt) => Future.successful(userOpt),
    () => userDao.save(civiUserToUser(contact)).map(Some(_))
  )

  private def civiUserToUser(civiContact: CiviContact) : User =
    User(
      UUID.randomUUID(),
      List(
        Profile(LoginInfo("civi", civiContact.email),true, Some(civiContact.email),
          Supporter(
            firstName = if(civiContact.firstName != "") Some(civiContact.firstName) else None,
            lastName = if(civiContact.lastName != "") Some(civiContact.lastName) else None,
            fullName = if(civiContact.displayName != "") Some(civiContact.displayName) else None,
            username = if(civiContact.nickname != "") Some(civiContact.nickname) else None,
            mobilePhone = if(civiContact.phone != "") Some(civiContact.phone) else None,
            placeOfResidence = if(civiContact.place_of_residence != "") Some(civiContact.place_of_residence) else None,
            birthday = civiContact.birth_date.map(_.getTime),
            sex = civiContact.gender.map(_.stringify),
            crew = None,
            pillars = Set()
          ),
          None,
          None,
          civiContact.image_url.map((uri) => List(CiviProfileImage(uri.toURL))).getOrElse(Nil)
        )
      )
    )
}