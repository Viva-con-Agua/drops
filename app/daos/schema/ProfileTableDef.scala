package daos.schema

import slick.driver.MySQLDriver.api._

import models.database.ProfileDB

class ProfileTableDef(tag: Tag) extends Table[ProfileDB](tag, "Profile") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def confirmed = column[Boolean]("confirmed")
  def email = column[String]("email")
  def userId = column[Long]("user_id")

  def * =
    (id, confirmed, email, userId)<>((ProfileDB.mapperTo _).tupled, ProfileDB.unapply)

  def userKey = foreignKey("user_id", userId, TableQuery[UserTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)

}
