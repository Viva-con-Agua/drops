package models.database

import java.sql.Blob
import java.util.UUID
import javax.imageio.ImageIO
import java.io.ByteArrayOutputStream

import models.UploadedImage

case class UploadDB(id: Long,
                    publicId: UUID,
                    parentId: Option[UUID],
                    name: String,
                    contentType: String,
                    width: Int,
                    height: Int,
                    data: Blob,
                    email: String,
                    selected: Boolean
                   ) {
  def toUploadedImage(thumbnails : List[UploadedImage] = Nil) : UploadedImage = {
    val in = data.getBinaryStream
    val bi = javax.imageio.ImageIO.read(in)
    val ui = UploadedImage(publicId, selected, name, contentType, bi)
    ui.addThumbs(thumbnails)
  }
}

object UploadDB extends ((Long, UUID, Option[UUID], String, String, Int, Int, Blob, String, Boolean) => UploadDB) {
  def apply(t: (Long, UUID, Option[UUID], String, String, Int, Int, Blob, String, Boolean)): UploadDB =
    UploadDB(t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8, t._9, t._10)

  def apply(uploadedImage: UploadedImage, email: String, selected: Boolean, parentId: Option[UUID] = None): UploadDB = {
    val baos = new ByteArrayOutputStream
    ImageIO.write(uploadedImage.bufferedImage, uploadedImage.format, baos)
    baos.flush()
    val imageInByte = baos.toByteArray
    baos.close()

    val blob = new javax.sql.rowset.serial.SerialBlob(imageInByte)
    UploadDB(0, uploadedImage.uuid, parentId, uploadedImage.getName, uploadedImage.getContentType, uploadedImage.width,
      uploadedImage.height, blob, email, selected)
  }
}