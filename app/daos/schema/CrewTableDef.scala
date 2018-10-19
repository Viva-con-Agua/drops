package daos.schema

import java.util.UUID

import models.database.CrewDB
import slick.driver.MySQLDriver.api._

class CrewTableDef(tag: Tag) extends Table[CrewDB](tag, "Crew") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def publicId = column[UUID]("publicId")
  def name = column[String]("name")

  def * =
    (id, publicId, name) <>(CrewDB.tupled, CrewDB.unapply)
}
