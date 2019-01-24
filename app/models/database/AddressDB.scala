package models.database

import models.Profile

import java.util.UUID

import models.{Organization, BankAccount}

case class AddressDB(
  id: Long,
  publicId: UUID,
  supporterId: UUID,
  street: String,
  additional: String,
  zip: String,
  city: String,
  country: String,
  ){
    def toAdress(Address = Address(this.publicId, this.supporterId, this.street, this.addititonal, this.zip, this.city, this.country)
}

object AddressDB {
  
 // def toOrganization : Organization = Organization(this.publicId, this.name, this.address, this.telefon, this.fax, this.email, this.executive, this.abbreviation, this.impressum)

  def mapperTo(
    id: Long, publicId: UUID, supporterId: Long, street: String, additional: String, zip: String, city: String, country: String
  ) = apply(id, publicId, supporterId, street, additional, zip, city, country)

  
  def apply(id: Long, address: Address):AddressDB = AddressDB(id, address.publicId, address.supporterId, address.street, address.additional, address.zip, address.city, address.country)

  def apply(Address: Address):AddressDB = AddressDB(0, address.publicId, address.supporterId, address.street, address.additional, address.zip, address.city, address.country)

}
