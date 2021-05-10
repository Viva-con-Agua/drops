package daos.schema

import java.util.UUID

import models.database.AddressDB
import slick.driver.MySQLDriver.api._

class AddressTableDef(tag: Tag) extends Table[AddressDB](tag, "Address") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def publicId = column[UUID]("public_id")
  def street = column[String]("street")
  def additional = column[String]("additional")
  def zip = column[String]("zip")
  def city = column[String]("city")
  def country = column[String]("country")
  def supporterId = column[Long]("supporter_id")

  def * =
    (id, publicId, street, additional.?, zip, city, country, supporterId) <> (AddressDB.tupled, AddressDB.unapply)

  def supporterKey = foreignKey("supporter_id", supporterId, TableQuery[SupporterTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)

}
