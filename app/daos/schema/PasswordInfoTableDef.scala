package daos.schema

import slick.driver.MySQLDriver.api._

import models.database.PasswordInfoDB

class PasswordInfoTableDef(tag: Tag) extends Table[PasswordInfoDB](tag, "PasswordInfo") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def hasher = column[String]("hasher")
  def password = column[String]("password")
  def profileId = column[Long]("profile_id")

  def * =
    (id, hasher, password, profileId) <> (PasswordInfoDB.tupled, PasswordInfoDB.unapply)

  def profileKey = foreignKey("profile_id", profileId, TableQuery[ProfileTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)

}
