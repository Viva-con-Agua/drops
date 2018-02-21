package daos.schema

import slick.driver.MySQLDriver.api._
import models.database.OauthClientDB

class OauthClientTableDef(tag: Tag) extends Table[OauthClientDB](tag, "OauthClient") {
  def id = column[String]("id", O.PrimaryKey)
  def secret = column[String]("secret")
  def redirectUri = column[String]("redirectUri")
  def grantTypes = column[String]("grantTypes")

  def * =
    (id, secret, redirectUri.?, grantTypes) <>((OauthClientDB.mapperTo _).tupled, OauthClientDB.unapply)
}
