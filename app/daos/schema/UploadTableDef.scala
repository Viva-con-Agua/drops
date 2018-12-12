package daos.schema

import java.sql.Blob
import java.util.UUID

import slick.driver.MySQLDriver.api._

/**
  * Definition of the UPLOAD table.
  */
class UploadTableDef(tag: Tag) extends Table[(Long, UUID, Option[UUID], String, String, Int, Int, Blob, String)](tag, "Upload") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def publicId = column[UUID]("public_id")
  def parentId = column[Option[UUID]]("parent_id")
  def name = column[String]("name")
  def contentType = column[String]("contentType")
  def width = column[Int]("width")
  def height = column[Int]("height")
  def data = column[Blob]("data")
  def email = column[String]("email")
  def * = (id, publicId, parentId, name, contentType, width, height, data, email)

  def pk = primaryKey("primaryKey", id)
}
//val uploads = TableQuery[Upload]