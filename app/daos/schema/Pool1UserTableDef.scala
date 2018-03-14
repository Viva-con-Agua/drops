package daos.schema

import slick.driver.MySQLDriver.api._

import models.database.Pool1UserDB

class Pool1UserTableDef(tag: Tag) extends Table[Pool1UserDB](tag, "Pool1User") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def email = column[String]("email")
  def confirmed = column[Boolean]("confirmed")

  def * = (id, email, confirmed) <>(Pool1UserDB.tupled, Pool1UserDB.unapply)
}
