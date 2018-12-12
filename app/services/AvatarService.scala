package services

import javax.inject._
import java.io.File
import java.util.UUID

import daos.AvatarDao
import models.UploadedImage

import scala.concurrent.Future

class AvatarService @Inject() (avatarDao : AvatarDao) {

  def getAll : Future[List[UploadedImage]] = avatarDao.getAll

  def get(uuid: UUID): Future[Option[UploadedImage]] = avatarDao.get(uuid)

  def getThumb(uuid: UUID, width: Int, height: Int): Future[Option[UploadedImage]] =
    avatarDao.getThumb(uuid, width, height)

  def add(image: File, fileName: String, contentType: Option[String], email: String): Future[Option[UploadedImage]] =
    avatarDao.add(image, fileName, contentType, email)

  def replaceThumbs(uuid: UUID, thumbs: List[UploadedImage], email: String): Future[Either[Exception, List[UploadedImage]]] =
    avatarDao.replaceThumbs(uuid, thumbs, email)

  def remove(uuid: UUID) : Future[Int] = avatarDao.remove(uuid)
}
