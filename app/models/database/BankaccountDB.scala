package models.database

import models.Bankaccount

case class BankaccountDB(
    id: Long,
    bankName: String,
    number: Option[String],
    blz: Option[String],
    iban: String,
    bic: String,
    organization_id: Long
  ){
    def toBankaccount : Bankaccount = Bankaccount(this.bankName, this.number, this.blz, this.iban, this.bic)
  }
object BankaccountDB extends ((Long, String, Option[String], Option[String], String, String, Long) => BankaccountDB){
}
