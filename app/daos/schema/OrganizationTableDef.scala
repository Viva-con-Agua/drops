package daos.schema

import java.util.UUID

import models.database.OrganizationDB
import slick.driver.MySQLDriver.api._

class OrganizationTableDef(tag: Tag) extends Table[OrganizationDB](tag, "Organization") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def publicId = column[UUID]("publicId")
  def name = column[String]("name")
  def address = column[String]("address")
  def telefon = column[String]("telefon")
  def fax = column[String]("fax")
  def email = column[String]("email")
  def executive = column[String]("executive")
  def abbreviation = column[String]("abbreviation")
  def impressum = column[String]("impressum")

  def * = (id, publicId, name, address, telefon, fax, email, executive, abbreviation, impressum)<>((OrganizationDB.mapperTo _).tupled, OrganizationDB.unapply)
}
