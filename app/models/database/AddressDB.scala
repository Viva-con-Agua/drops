package models.database

import models.Address

import java.util.UUID

import models.{Organization, BankAccount}


/**
 * case class for AddressDB
 * @param id mysql identifire as Long
 * @param publicId uuid as identifire for public using as UUID
 * @param street street path of the addree as String
 * @param additional additional informations as String
 * @param zip zip number as String
 * @param city city name as String
 * @param country country name as String
 * @param supporterId SupporterDB id as foreignKey as Long 
 * @param toAddress convert the AddressDB model to Address model as method
 */
case class AddressDB(
  id: Long,
  publicId: Option[UUID],
  street: String,
  additional: Option[String],
  zip: String,
  city: String,
  country: String,
  supporterId: Long
  ){
    def toAddress: Address = 
      Address(
        this.publicId, 
        this.street, 
        this.additional, 
        this.zip, 
        this.city, 
        this.country
      )
}

  object AddressDB extends ((Long, Option[UUID], String, Option[String], String, String, String, Long) => AddressDB) {

  def apply(tuple: (Long, Option[UUID], String, Option[String], String, String, String, Long)): AddressDB =
    AddressDB(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7, tuple._8)

  def apply(id: Long, address: Address, supporterId: Long):AddressDB = AddressDB(id, address.publicId, address.street, address.additional, address.zip, address.city, address.country, supporterId)

  def apply(id: Long, uuid: Option[UUID], address: Address, supporterId: Long):AddressDB = AddressDB(id, uuid, address.street, address.additional, address.zip, address.city, address.country, supporterId)

  def apply(address: Address, supporterId: Long):AddressDB = AddressDB(0, address.publicId, address.street, address.additional, address.zip, address.city, address.country, supporterId)
  
}
