package models.converter

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo

import models._
import models.database._
import play.api.Logger


object OrganizationConverter {
  
  def buildOrganizationFromResult(result : Seq[(OrganizationDB, ProfileDB)]) : Option[Organization] = {
    if(result.headOption.isDefined) {
      val organization = result.headOption.get._1
      val profileList = result.seq.foldLeft(Set[String]()) { (profileList, dbEntry) => {
       Logger.debug(s"dbEnty=${dbEntry._2.email}")
        profileList ++ List(dbEntry._2.email)
    }}
    Logger.debug(s"profileList= $profileList")
      Option(
        Organization(
          organization.publicId, 
          organization.name, 
          organization.address, 
          organization.telefon, 
          organization.fax, 
          organization.email,
          organization.executive,
          organization.abbreviation,
          organization.impressum,
          None,
          Some(profileList)))
    }else{
      None
    }
  }

  def buildOrganizationBankaccountFromResult(result: Seq[(OrganizationDB, BankaccountDB)]) : Option[Organization] = {
    if(result.headOption.isDefined) {
      val organization = result.headOption.get._1
      val bankaccountList = result.seq.foldLeft(Set[Bankaccount]()) { 
        (bankaccountList, dbEntry) => {
          bankaccountList ++ List(dbEntry._2.toBankaccount)
        }
      }
      Option(
        Organization(
          organization.publicId, 
          organization.name, 
          organization.address, 
          organization.telefon, 
          organization.fax, 
          organization.email,
          organization.executive,
          organization.abbreviation,
          organization.impressum,
          Some(bankaccountList),
          None
        )
      )
    }else{
      None
    }
  }
}

