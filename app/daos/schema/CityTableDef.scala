package daos.schema

import slick.driver.MySQLDriver.api._

import models.database.CityDB

class CityTableDef(tag: Tag) extends Table[CityDB](tag, "City") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def country = column[String]("country")
  def crewId = column[Long]("crew_id")

  def * =
    (id, name, country, crewId) <>(CityDB.tupled, CityDB.unapply)

  def crewKey = foreignKey("crew_id", crewId, TableQuery[CrewTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)
}
