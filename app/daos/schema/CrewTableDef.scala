package daos.schema

import java.util.UUID

import models.database.CrewDB
import slick.driver.MySQLDriver.api._

class CrewTableDef(tag: Tag) extends Table[CrewDB](tag, "Crew") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def publicId = column[UUID]("publicId")
  def name = column[String]("name")
  def country =  column[String]("country")

  def * =
    (id, publicId, name, country) <>((CrewDB.mapperTo _).tupled, CrewDB.unapply)
}
