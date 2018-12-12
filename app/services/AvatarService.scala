package services

import javax.inject._
import java.io.File
import java.util.UUID

import daos.{AvatarDao, UserDao}
import models.{Profile, UploadedImage}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AvatarService @Inject() (avatarDao : AvatarDao, userDao: UserDao) {

  def getAll(profile: Profile) : Future[List[UploadedImage]] = profile.email match {
    case Some(email) => avatarDao.getAll(email)
    case _ => Future.successful(Nil)
  }

  def getSelected(userUUID: UUID, width: Int, height: Int) : Future[Option[UploadedImage]] = userDao.find(userUUID).flatMap(_ match {
    case Some(user) => user.profiles.headOption match {
      case Some(profile) => profile.email match {
        case Some(email) => avatarDao.getSelected(email, width: Int, height: Int)
        case None => Future.successful(None)
      }
      case None => Future.successful(None)
    }
    case None => Future.successful(None)
  })

  def get(uuid: UUID, profile: Profile): Future[Option[UploadedImage]] = {
    avatarDao.get(uuid, profile.email.get)
  }

  def getThumb(uuid: UUID, width: Int, height: Int, profile: Profile): Future[Option[UploadedImage]] =
    profile.email match {
      case Some(email) => avatarDao.getThumb(uuid, width, height, email)
      case _ => Future.successful(None)
    }

  def select(uuid: UUID, profile: Profile) = profile.email match {
    case Some(email) => avatarDao.select(uuid, email)
    case None => Future.successful(false)
  }

  def add(image: File, fileName: String, contentType: Option[String], profile: Profile, selected: Boolean): Future[Option[UploadedImage]] =
    profile.email match {
      case Some(email) => avatarDao.add(image, fileName, contentType, email, selected)
      case _ => Future.successful(None)
    }

  def replaceThumbs(uuid: UUID, thumbs: List[UploadedImage], profile: Profile): Future[Either[Exception, List[UploadedImage]]] =
    profile.email match {
      case Some(email) => avatarDao.replaceThumbs(uuid, thumbs, email)
      case _ => Future.successful(Left(new Exception("Found no email for the given profile.")))
    }

  def updateEmail(previousEmail: String, newEmail: String) : Future[Int] = avatarDao.updateEmail(previousEmail, newEmail)

  def remove(uuid: UUID, profile: Profile) : Future[Int] = profile.email match {
    case Some(email) => avatarDao.get(uuid, email).flatMap(_ match {
      case Some(uploadedImage) if uploadedImage.selected => avatarDao.remove(uuid, email).flatMap(_ match {
        case x if x > 0 => avatarDao.getAll(email).flatMap(_.headOption match {
          case Some(otherImage) => avatarDao.select(otherImage.uuid, email).map(_ => x)
          case None => Future.successful(x)
        })
        case x => Future.successful(x)
      })
      case _ => avatarDao.remove(uuid, email)
    })
    case _ => Future.successful(0)
  }
}
