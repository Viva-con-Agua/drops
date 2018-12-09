package models

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
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

    // tokenize the data
    val parts = base64.split(",")
    val decoder = new BASE64Decoder
    val imageByte = decoder.decodeBuffer(parts(1))
    val bis = new ByteArrayInputStream(imageByte)
    val image = ImageIO.read(bis)

//    val data = Base64.decodeBase64(base64)
//    val bis = new ByteArrayInputStream(data)

    // only get the last part of the filename
    // otherwise someone can send a path like ../../home/foo/bar.txt to write to other files on the system
    //      import java.io.File
    //      val file = picture.ref.file //.moveTo(new File(s"tmp/picture/$filename"))
//    def img = ImageIO.read(bis)
    UploadedImage(name, contentType, base64, image, Nil)
  }
}

case class RESTImageRequest(id: String, contentType: String, base64: String) {
  def toUploadedImage: UploadedImage = UploadedImage(id, contentType, base64)
}
object RESTImageRequest {
  implicit val restImageFormat = Json.format[RESTImageRequest]
}

case class RESTImageResponse(url: String, id: String, width: Int, height: Int)
object RESTImageResponse {
  implicit val restImageResponse = Json.format[RESTImageResponse]
}