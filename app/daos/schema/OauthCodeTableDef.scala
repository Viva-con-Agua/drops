package daos.schema

import java.util.UUID

import slick.driver.MySQLDriver.api._
import models.database.OauthCodeDB
import org.joda.time.DateTime

class OauthCodeTableDef(tag: Tag) extends Table[OauthCodeDB](tag, "OauthCode") {
  def code = column[String]("code")
  def userId = column[UUID]("user_id")
  def clientId = column[String]("client_id")
  def created = column[DateTime]("created")

  implicit val dateColumnType =
    MappedColumnType .base[DateTime, Long] (
      e => e.getMillis,
      e => new DateTime(e)
    )

  def pk = primaryKey("primaryKey", (userId, clientId))

  def uK = foreignKey("user_id", userId, TableQuery[UserTableDef])(_.publicId, onUpdate = ForeignKeyAction.Cascade)
  def cK = foreignKey("client_id", clientId, TableQuery[OauthClientTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)

  def * =
    (code, userId, clientId, created) <>((OauthCodeDB.mapperTo _).tupled, OauthCodeDB.unapply)

}
