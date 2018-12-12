package services

import javax.inject._
import java.io.File
import java.util.UUID

import daos.AvatarDao
import models.UploadedImage

import scala.concurrent.Future

class AvatarService @Inject() (avatarDao : AvatarDao) {

  def getAll(email: String) : Future[List[UploadedImage]] = avatarDao.getAll(email)

  def get(uuid: UUID, email: String): Future[Option[UploadedImage]] = avatarDao.get(uuid, email)

  def getThumb(uuid: UUID, width: Int, height: Int, email: String): Future[Option[UploadedImage]] =
    avatarDao.getThumb(uuid, width, height, email)

  def add(image: File, fileName: String, contentType: Option[String], email: String): Future[Option[UploadedImage]] =
    avatarDao.add(image, fileName, contentType, email)

  def replaceThumbs(uuid: UUID, thumbs: List[UploadedImage], email: String): Future[Either[Exception, List[UploadedImage]]] =
    avatarDao.replaceThumbs(uuid, thumbs, email)

  def updateEmail(previousEmail: String, newEmail: String) : Future[Int] = avatarDao.updateEmail(previousEmail, newEmail)

  def remove(uuid: UUID, email: String) : Future[Int] = avatarDao.remove(uuid, email)
}
