package daos.schema

import java.sql.Timestamp
import java.util.{Date, UUID}

import slick.driver.MySQLDriver.api._
import models.database.OauthTokenDB

class OauthTokenTableDef(tag: Tag) extends Table[OauthTokenDB](tag, "OauthToken") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def token = column[String]("token")
  def refreshToken = column[String]("refresh_token")
  def scope = column[String]("scope")
  def lifeSeconds = column[Long]("life_seconds")
  def createdAt = column[Date]("created_at")

  def userId = column[UUID]("user_id")
  def clientId = column[String]("client_id")

  implicit val dateColumnType =
    MappedColumnType .base[Date, Timestamp] (
      d => new Timestamp(d.getTime),
      d => new Date(d.getTime)
    )

  def uK = foreignKey("user_id", userId, TableQuery[UserTableDef])(_.publicId, onUpdate = ForeignKeyAction.Cascade)
  def cK = foreignKey("client_id", clientId, TableQuery[OauthClientTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)

  def * =
    (id, token, refreshToken.?, scope.?, lifeSeconds.?, createdAt, userId, clientId)<>(OauthTokenDB.tupled, OauthTokenDB.unapply)

}
