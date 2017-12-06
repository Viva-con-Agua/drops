package daos.schema

import slick.driver.MySQLDriver.api._

import models.database.LoginInfo

class LoginInfoTableDef(tag: Tag) extends Table[LoginInfo](tag, "LoginInfo") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def providerId = column[String]("provider_id")
  def providerKey = column[String]("provider_key")
  def profileId = column[Long]("profile_id")

  def * =
    (id, providerId, providerKey, profileId) <> ((LoginInfo.mapperTo _).tupled, LoginInfo.unapply)

  def profileKey = foreignKey("profile_id", profileId, TableQuery[ProfileTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)

}
