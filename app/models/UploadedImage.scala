package models

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File}
import java.util.Base64

import sun.misc.BASE64Decoder
import javax.imageio.ImageIO
import play.api.libs.json.Json

case class UploadedImage(name: String, contentType : String, base64: String, bufferedImage : BufferedImage, thumbnails : List[UploadedImage]) {
  val width = bufferedImage.getWidth()
  val height = bufferedImage.getHeight()

  val format = contentType.split("/").lastOption.getOrElse("png")

  def getName : String = name.split(".").lastOption match {
    case Some(suffix) if suffix == format =>
      (name.split(".").reverse.tail.reverse ++ List((width + "x" + height), format)).mkString(".")
    case _ => List(name, (width + "x" + height), format).mkString(".")
  }

  def getContentType: String = contentType match {
    case "png" => "image/png"
    case _ => contentType
  }

  def getRESTResponse(url: String) : RESTImageResponse = RESTImageResponse(url, name, width, height)

  def output: ByteArrayOutputStream = {
    val baos = new ByteArrayOutputStream()
    ImageIO.write(bufferedImage, contentType, baos)
    return baos
  }

  def ~(name: String, width: Int, height: Int) : Boolean =
    this.name == name && this.width == width && this.height == height
}

object UploadedImage {
  def apply(name: String, contentType : String, base64: String): UploadedImage = {
    val parts = base64.split(",")
    val decoder = new BASE64Decoder
    val imageByte = decoder.decodeBuffer(parts(1))
    val bis = new ByteArrayInputStream(imageByte)
    val image = ImageIO.read(bis)

    UploadedImage(name, contentType, base64, image, Nil)
  }

  def apply(file: File, name: Option[String], contentType: Option[String] = None): UploadedImage = {
    val bufferedImage = ImageIO.read(file)
    val n = name.getOrElse(file.getName)
    val format = contentType.flatMap(_.split("/").lastOption).getOrElse("png")
    val ct = contentType.getOrElse("image/" + format)
    val os = new ByteArrayOutputStream
    ImageIO.write(bufferedImage, format, os)
    val base64 = Base64.getEncoder().encodeToString(os.toByteArray())

    UploadedImage(n, ct, base64, bufferedImage, Nil)
  }
}

case class RESTImageRequest(id: String, contentType: String, base64: String) {
  def toUploadedImage: UploadedImage = UploadedImage(id, contentType, base64)
}
object RESTImageRequest {
  implicit val restImageFormat = Json.format[RESTImageRequest]
}

case class RESTMetaRequest(id: String, contentType: String)
object RESTMetaRequest {
  implicit val restMetaFormat = Json.format[RESTMetaRequest]
}

case class RESTImageResponse(url: String, id: String, width: Int, height: Int)
object RESTImageResponse {
  implicit val restImageResponse = Json.format[RESTImageResponse]
}