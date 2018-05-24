package models.database

case class BankaccountDB(
    id: Long,
    bankName: String,
    number: Option[String],
    blz: Option[String],
    iban: String,
    bic: String,
    organization_id: Long
  )
object BankaccountDB extends ((Long, String, Option[String], Option[String], String, String, Long) => BankaccountDB){}
