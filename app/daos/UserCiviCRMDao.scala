package daos

import java.net.URI
import java.util.UUID
import javax.inject.Inject

import civiretention._
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

  def save(user: User)(implicit messages: Messages) : Future[List[User]] = this.userToCiviUser(user).flatMap(_.map(
    (civiContactContainer) => contactResolver.create(civiContactContainer)
  ).getOrElse(Future.successful(Nil))).flatMap((l) => Future.sequence(l.map(mapper( _ )(
    (userOpt) => Future.successful(userOpt),
    () => Future.successful(None)
  ))).map(_.filter(_.isDefined).map(_.get)))

  private def mapper(contact: CiviContactContainer)(success: Option[User] => Future[Option[User]], failure: () => Future[Option[User]]) : Future[Option[User]] = contact.contact.contactType match {
    case Individual => userDao.find(contact.contact.email).flatMap((res) => res.isDefined match {
      case true => success(res)
      case false => failure()
    } )
    case _ => Future.successful(None)
  }

  private def findOrSave(contact: CiviContactContainer) : Future[Option[User]] = mapper(contact)(
    (userOpt) => Future.successful(userOpt),
    () => userDao.save(civiUserToUser(contact)).map(Some(_))
  )

  private def civiUserToUser(civiContact: CiviContactContainer) : User = {
    val email = civiContact.getEmail match {
      case Some(email) => email.address
      case None => civiContact.contact.email
    }
    User(
      UUID.randomUUID(),
      List(
        Profile(
          LoginInfo("civi",email),
          true,
          Some(email),
          Supporter(
            firstName = if (civiContact.contact.firstName != "") Some(civiContact.contact.firstName) else None,
            lastName = if (civiContact.contact.lastName != "") Some(civiContact.contact.lastName) else None,
            fullName = if (civiContact.contact.displayName != "") Some(civiContact.contact.displayName) else None,
            username = if (civiContact.contact.nickname != "") Some(civiContact.contact.nickname) else None,
            mobilePhone = civiContact.getMobilePhone match {
              case Some(phone) => Some(phone.phone)
              case None if civiContact.contact.phone != "" => Some(civiContact.contact.phone)
              case _ => None
            },
            placeOfResidence = civiContact.getAddress match {
              case Some(address) => Some(address.city)
              case None if civiContact.contact.place_of_residence != "" => Some(civiContact.contact.place_of_residence)
              case _ => None
            },
            birthday = civiContact.contact.birth_date.map(_.getTime),
            sex = civiContact.contact.gender.map(_.stringify),
            crew = None,
            pillars = Set()
          ),
          None,
          None,
          civiContact.contact.image_url.map((uri) => List(CiviProfileImage(uri.toURL))).getOrElse(Nil)
        )
      )
    )
  }

  private def userToCiviUser(user: User) : Future[Option[CiviContactContainer]] = {
    val phones = CiviPhone(user)
    val emails = CiviEmail(user)
    val addresses = CiviAddress(user)
    val contact = CiviContact(user)

    contact.map(_.map(CiviContactContainer(_, phones, emails, addresses)))
  }
}