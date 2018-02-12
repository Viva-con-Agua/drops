package daos.schema

import slick.driver.MySQLDriver.api._

import models.database.RoleDB

class RoleTableDef (tag: Tag) extends Table[RoleDB](tag, "Role") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def role = column[String]("role")

  def * =
    (id, role) <> ((RoleDB.mapperTo _).tupled, RoleDB.unapply)
}
