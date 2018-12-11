package models

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File}
import java.util.{Base64, UUID}

import sun.misc.BASE64Decoder
import javax.imageio.ImageIO
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, OWrites, Reads}

case class UploadedImage(uuid: UUID, name: String, contentType : String, base64: String, bufferedImage : BufferedImage, thumbnails : List[UploadedImage]) {
  val width = bufferedImage.getWidth()
  val height = bufferedImage.getHeight()

  val format = contentType.split("/").lastOption.getOrElse("png")

  def getID: UUID = this.uuid
//  def getName : String = name.split(".").lastOption match {
//    case Some(suffix) if suffix == format =>
//      (name.split(".").reverse.tail.reverse ++ List((width + "x" + height), format)).mkString(".")
//    case _ => List(name, (width + "x" + height), format).mkString(".")
//  }
  def getName: String = this.uuid.toString + "." + this.format

  def getContentType: String = contentType match {
    case "png" => "image/png"
    case _ => contentType
  }

  def output: ByteArrayOutputStream = {
    val baos = new ByteArrayOutputStream()
    ImageIO.write(bufferedImage, contentType, baos)
    return baos
  }

  def ~(width: Int, height: Int) : Boolean =
    this.width == width && this.height == height

  def addThumbs(thumbs: Seq[UploadedImage]) = this.copy(thumbnails = this.thumbnails ++ thumbs.toList)
  def replaceThumbs(thumbs: Seq[UploadedImage]) = this.copy(thumbnails = thumbs.toList)
}

object UploadedImage {
  def apply(name: String, contentType : String, base64: String): UploadedImage = {
    val parts = base64.split(",")
    val decoder = new BASE64Decoder
    val imageByte = decoder.decodeBuffer(parts(1))
    val bis = new ByteArrayInputStream(imageByte)
    val image = ImageIO.read(bis)

    UploadedImage(UUID.randomUUID(), name, contentType, base64, image, Nil)
  }

  def apply(file: File, name: Option[String], contentType: Option[String] = None): UploadedImage = {
    val bufferedImage = ImageIO.read(file)
    val n = name.getOrElse(file.getName)
    val format = contentType.flatMap(_.split("/").lastOption).getOrElse("png")
    val ct = contentType.getOrElse("image/" + format)
    val os = new ByteArrayOutputStream
    ImageIO.write(bufferedImage, format, os)
    val base64 = Base64.getEncoder().encodeToString(os.toByteArray())

    UploadedImage(UUID.randomUUID(), n, ct, base64, bufferedImage, Nil)
  }
}

case class RESTImageThumbnailResponse(url: String, id: UUID, width: Int, height: Int)
object RESTImageThumbnailResponse {
  def apply(uploadedImage: UploadedImage, parentID: UUID) : RESTImageThumbnailResponse = {
    val url = controllers.webapp.routes.Avatar.getThumb(parentID.toString, uploadedImage.width, uploadedImage.height).url
    RESTImageThumbnailResponse(url, uploadedImage.getID, uploadedImage.width, uploadedImage.height)
  }

  def apply(t: (String, UUID, Int, Int)): RESTImageThumbnailResponse =
    RESTImageThumbnailResponse(t._1, t._2, t._3, t._4)

  implicit val restImageThumbnailWrites : OWrites[RESTImageThumbnailResponse] = (
    (JsPath \ "url").write[String] and
      (JsPath \ "id").write[UUID] and
      (JsPath \ "width").write[Int] and
      (JsPath \ "height").write[Int]
    )(unlift(RESTImageThumbnailResponse.unapply))
  implicit val restImageThumbnailReads : Reads[RESTImageThumbnailResponse] = (
    (JsPath \ "url").read[String] and
      (JsPath \ "id").read[UUID] and
      (JsPath \ "width").read[Int] and
      (JsPath \ "height").read[Int]
    ).tupled.map(RESTImageThumbnailResponse( _ ))
}

case class RESTImageResponse(url: String, id: UUID, width: Int, height: Int, thumbnails: List[RESTImageThumbnailResponse])
object RESTImageResponse {
  def apply(uploadedImage: UploadedImage): RESTImageResponse = {
    val url = controllers.webapp.routes.Avatar.get(uploadedImage.getID.toString).url
    RESTImageResponse(
      url, uploadedImage.getID, uploadedImage.width, uploadedImage.height,
      uploadedImage.thumbnails.map(RESTImageThumbnailResponse(_, uploadedImage.getID))
    )
  }

  def apply(t: (String, UUID, Int, Int, List[RESTImageThumbnailResponse])): RESTImageResponse =
    RESTImageResponse(t._1, t._2, t._3, t._4, t._5)

  implicit val restImageWrites : OWrites[RESTImageResponse] = (
    (JsPath \ "url").write[String] and
      (JsPath \ "id").write[UUID] and
      (JsPath \ "width").write[Int] and
      (JsPath \ "height").write[Int] and
      (JsPath \ "thumbnails").write[List[RESTImageThumbnailResponse]]
    )(unlift(RESTImageResponse.unapply))
  implicit val restImageReads : Reads[RESTImageResponse] = (
    (JsPath \ "url").read[String] and
      (JsPath \ "id").read[UUID] and
      (JsPath \ "width").read[Int] and
      (JsPath \ "height").read[Int] and
      (JsPath \ "thumbnails").read[List[RESTImageThumbnailResponse]]
    ).tupled.map(RESTImageResponse( _ ))
}