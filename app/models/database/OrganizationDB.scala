package models.database

import java.util.UUID

import models.Organization

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
    def toOrganization(users : Option[Set[String]]) : Organization = Organization(this.publicId, this.name, this.address, this.telefon, this.fax, this.email, this.executive, this.abbreviation, this.impressum, users)
    
}

object OrganizationDB {
  
 // def toOrganization : Organization = Organization(this.publicId, this.name, this.address, this.telefon, this.fax, this.email, this.executive, this.abbreviation, this.impressum)

  def mapperTo(
    id: Long, publicId: UUID, name: String, address: String, telefon: String, fax: String, email: String, executive: String, abbreviation: String, impressum: String
  ) = apply(id, publicId, name, address, telefon, fax, email, executive, abbreviation, impressum)

  def apply(orgnization: Organization):OrganizationDB = OrganizationDB(0, orgnization.id, orgnization.name, orgnization.address, orgnization.telefon, orgnization.fax, orgnization.email, orgnization.executive, orgnization.abbreviation, orgnization.impressum)
}
