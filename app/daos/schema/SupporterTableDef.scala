package daos.schema

import slick.driver.MySQLDriver.api._

import models.database.SupporterDB

class SupporterTableDef(tag: Tag) extends Table[SupporterDB](tag, "Supporter") {
  def id                = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def firstName         = column[String]("first_name")
  def lastName          = column[String]("last_name")
  def fullName          = column[String]("full_name")
  def mobilePhone       = column[String]("mobile_phone")
  def placeOfResidence  = column[String]("place_of_residence")
  def birthday          = column[Long]("birthday")
  def sex               = column[String]("sex")
  def profileId         = column[Long]("profile_id")
  def addressId         = column[Long]("address_id")

  def * =
    (id, firstName.?, lastName.?, fullName.?, mobilePhone.?, placeOfResidence.?, birthday.?, sex.?, profileId, addressId.?)<>(SupporterDB.tupled, SupporterDB.unapply)

  def profileKey = foreignKey("profile_id", profileId, TableQuery[ProfileTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)
  def addressKey = foreignKey("address_id", addressId, TableQuery[AddressTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)

}
