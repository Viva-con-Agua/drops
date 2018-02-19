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
  def crewId            = column[Long]("crew_id")

  def * =
    (id, firstName.?, lastName.?, fullName.?, mobilePhone.?, placeOfResidence.?, birthday.?, sex.?, profileId, crewId.?)<>((SupporterDB.mapperTo _).tupled, SupporterDB.unapply)

  def profileKey = foreignKey("profile_id", profileId, TableQuery[ProfileTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)
  def crewKey = foreignKey("crew_id", crewId, TableQuery[CrewTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)
}
