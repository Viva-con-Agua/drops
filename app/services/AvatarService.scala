package services

import java.io.File
import java.util.UUID

import models.UploadedImage

import scala.concurrent.Future

class AvatarService {
  var files : Map[UUID, UploadedImage] = Map()

  def getAll : Future[List[UploadedImage]] = Future.successful(this.files.map(_._2).toList)

  def get(uuid: UUID): Future[Option[UploadedImage]] = Future.successful(this.files.get(uuid))

  def getThumb(uuid: UUID, width: Int, height: Int): Future[Option[UploadedImage]] = {
    def finder(width: Int, height: Int, ui: UploadedImage) = ui ~ (width, height)
    Future.successful(this.files.get(uuid).flatMap(
      _.thumbnails.find((ui: UploadedImage) => finder(width, height, ui))
    ))
  }

  def add(image: File, fileName: String, contentType: Option[String]): Future[Option[UploadedImage]] = {
    val newImages = List(UploadedImage(image, Some(fileName), contentType))
    this.files = this.files ++ newImages.map(uploaded => (uploaded.getID -> uploaded))
    Future.successful(newImages.headOption)
  }

  def replaceThumbs(uuid: UUID, thumbs: List[UploadedImage]): Future[Either[Exception, List[UploadedImage]]] = {
    this.files.get(uuid) match {
      case Some(original) => {
        this.files = (this.files - uuid) + (uuid -> original.replaceThumbs(thumbs))
        Future.successful(Right(thumbs))
      }
      case _ => Future.successful(Left(new Exception))
    }
  }

  def remove(uuid: UUID) : Future[Int] = {
    this.files.contains(uuid) match {
      case true => {
        this.files = this.files - uuid
        Future.successful(1)
      }
      case false => Future.successful(0)
    }
  }
}
