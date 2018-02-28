package daos.schema

import java.util.UUID

import slick.driver.MySQLDriver.api._
import models.database.UserDB

class UserTableDef(tag: Tag) extends Table[UserDB](tag, "User") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def publicId = column[UUID]("public_id")
  def roles = column[String]("roles")

  def * = (id, publicId, roles) <>((UserDB.mapperTo _).tupled, UserDB.unapply)

  def pk = primaryKey("primaryKey", id)
}
