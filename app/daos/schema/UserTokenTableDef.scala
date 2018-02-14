package daos.schema

import java.util.UUID

import slick.driver.MySQLDriver.api._
import models.database.UserTokenDB
import org.joda.time.{DateTime, DateTimeZone}


class UserTokenTableDef(tag: Tag) extends Table[UserTokenDB](tag, "UserToken") {
  def id              = column[UUID]("id", O.PrimaryKey)
  def userId          = column[UUID]("user_id")
  def email           = column[String]("email")
  def expirationTime  = column[DateTime]("expiration_time")
  def isSignUp        = column[Boolean]("is_sign_up")

  implicit val dateColumnType =
    MappedColumnType .base[DateTime, Long] (
      e => e.getMillis,
      e => new DateTime(e)
    )


  def * =
    (id, userId, email, expirationTime, isSignUp)<>((UserTokenDB.mapperTo _).tupled, UserTokenDB.unapply)
}
