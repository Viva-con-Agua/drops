package models.database

import models.BankAccount

case class BankAccountDB(
    id: Long,
    bankName: String,
    number: Option[String],
    blz: Option[String],
    iban: String,
    bic: String,
    organization_id: Long
  ){
    def toBankAccount : BankAccount = BankAccount(this.bankName, this.number, this.blz, this.iban, this.bic)
  }
object BankAccountDB extends ((Long, String, Option[String], Option[String], String, String, Long) => BankAccountDB){
}
