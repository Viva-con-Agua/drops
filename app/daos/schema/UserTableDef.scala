package daos.schema

import java.util.UUID

import slick.driver.MySQLDriver.api._
import models.database.User

class UserTableDef(tag: Tag) extends Table[User](tag, "User") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def publicId = column[UUID]("public_id")

  def * = (id, publicId) <>((User.mapperTo _).tupled, User.unapply)

  def pk = primaryKey("primaryKey", id)
}
