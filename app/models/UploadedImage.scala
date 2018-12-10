package models

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File}
import java.util.{Base64, UUID}

import sun.misc.BASE64Decoder
import javax.imageio.ImageIO
import play.api.libs.json.Json

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

  def getRESTResponse(url: String) : RESTImageResponse = RESTImageResponse(url, uuid, width, height)

  def output: ByteArrayOutputStream = {
    val baos = new ByteArrayOutputStream()
    ImageIO.write(bufferedImage, contentType, baos)
    return baos
  }

  def ~(width: Int, height: Int) : Boolean =
    this.width == width && this.height == height

  def addThumbs(thumbs: Seq[UploadedImage]) = this.copy(thumbnails = this.thumbnails ++ thumbs.toList)
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

case class RESTImageResponse(url: String, id: UUID, width: Int, height: Int)
object RESTImageResponse {
  implicit val restImageResponse = Json.format[RESTImageResponse]
}