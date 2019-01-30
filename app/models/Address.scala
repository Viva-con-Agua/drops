package models

import java.util.UUID
import play.api.libs.json.Json

trait AddressBase {
  val street: String
  val additional: Option[String]
  val zip: String
  val city: String
  val country: String
}

/**
 * case class for AddressStub
 * @param street street path of the addree as String
 * @param additional additional informations as Option[String]
 * @param zip zip number as String
 * @param city city name as String
 * @param country country name as String
 * @param toAddress initial the AddressStub model as Address model with a random UUID
 */
case class AddressStub (
  street: String,
  additional: Option[String],
  zip: String,
  city: String,
  country: String

) extends AddressBase {
  def toAddress: Address = Address(UUID.randomUUID(), street, additional, zip, city, country)
}

/**
 * case class for AddressDB
 * @param publicId uuid as identifire for public using as UUID
 * @param street street path of the addree as String
 * @param additional additional informations as Option[String]
 * @param zip zip number as String
 * @param city city name as String
 * @param country country name as String
 * @param toAddressStub convert the Address to AddressStub
 */
case class Address (
  publicId: UUID,
  override val street: String,
  override val additional: Option[String],
  override val zip: String,
  override val city: String,
  override val country: String
) extends AddressBase {
  def toAddressStub(): AddressStub =
    AddressStub(street, additional, zip, city, country)
}

object Address {
  implicit val addressJsonFormat = Json.format[Address]
}

object AddressStub {
  implicit val addressJsonFormat = Json.format[AddressStub]
}

trait AddressUUIDBase {
  val publicId: UUID
}

case class AddressUUID (
  publicId: UUID
) extends AddressUUIDBase

object AddressUUID {
  implicit val addressUUIDJsonFormat = Json.format[AddressUUID]
}
