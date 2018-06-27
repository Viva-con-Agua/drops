package daos.schema

import slick.driver.MySQLDriver.api._

import models.database.BankaccountDB


class BankaccountTableDef(tag: Tag) extends Table[BankaccountDB](tag, "Bankaccount") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def bankName = column[String]("bankName")
  def number = column[Option[String]]("number")
  def blz = column[Option[String]]("blz")
  def iban = column[String]("iban")
  def bic = column[String]("bic")
  def organization_id = column[Long]("organization_id")

  def * =
    (id, bankName, number, blz, iban, bic, organization_id) <> (BankaccountDB.tupled, BankaccountDB.unapply)
  
  def organizationKey = foreignKey("organization_id", organization_id, TableQuery[OrganizationTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)
}
