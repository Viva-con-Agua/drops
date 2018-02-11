package daos.schema

import slick.driver.MySQLDriver.api._

import models.database.OAuth1InfoDB


class OAuth1InfoTableDef (tag: Tag) extends Table[OAuth1InfoDB](tag, "OAuth1Info") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def token = column[String]("token")
  def secret =  column[String]("secret")
  def profileId = column[Long]("profile_id")
  def * =
    (id, token, secret, profileId) <> ((OAuth1InfoDB.mapperTo _).tupled, OAuth1InfoDB.unapply)

  def profileKey = foreignKey("profile_id", profileId, TableQuery[ProfileTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)
}
