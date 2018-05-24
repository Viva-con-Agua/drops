package models.database

import models.Profile

import java.util.UUID

import models.{Organization, Bankaccount}

case class OrganizationDB(
  id: Long,
  publicId: UUID,
  name: String,
  address: String,
  telefon: String,
  fax: String,
  email: String,
  executive: String,
  abbreviation: String,
  impressum: String
  ){
    def toOrganization(bankaccount: Option[Set[Bankaccount]], profiles : Option[Set[String]]) : Organization = Organization(this.publicId, this.name, this.address, this.telefon, this.fax, this.email, this.executive, this.abbreviation, this.impressum, bankaccount, profiles)
    
}

object OrganizationDB {
  
 // def toOrganization : Organization = Organization(this.publicId, this.name, this.address, this.telefon, this.fax, this.email, this.executive, this.abbreviation, this.impressum)

  def mapperTo(
    id: Long, publicId: UUID, name: String, address: String, telefon: String, fax: String, email: String, executive: String, abbreviation: String, impressum: String
  ) = apply(id, publicId, name, address, telefon, fax, email, executive, abbreviation, impressum)

  
  def apply(id: Long, organization: Organization):OrganizationDB = OrganizationDB(id, organization.publicId, organization.name, organization.address, organization.telefon, organization.fax, organization.email, organization.executive, organization.abbreviation, organization.impressum)

  def apply(organization: Organization):OrganizationDB = OrganizationDB(0, organization.publicId, organization.name, organization.address, organization.telefon, organization.fax, organization.email, organization.executive, organization.abbreviation, organization.impressum)

}
