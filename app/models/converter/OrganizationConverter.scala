package models.converter

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo

import models._
import models.database._

object OrganizationConverter {
  
  def buildOrganizationFromResult(result : Seq[(OrganizationDB, ProfileDB)]) : Option[Organization] = {
    if(result.headOption.isDefined) {
      val organization = result.headOption.get._1
      val profileList = result.seq.foldLeft(Seq[String]()) { (profileList, dbEntry) => {
        profileList ++ List(dbEntry._2.email)
        profileList 
    }}
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
          profileList))
    }else{
      None
    }
  }
}

