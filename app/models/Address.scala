package models

import java.util.UUID
import play.api.libs.json.Json

trait AddressBase {
  val street: Option[String]
  val additional: Option[String]
  val zip: Option[String]
  val city: Option[String]
  val country: Option[String]
}

case class Address (
  street: Option[String],
  additional: Option[String],
  zip: Option[String],
  city: Option[String],
  country: Option[String]
) extends AddressBase

case class AddressStub (
  street: Option[String],
  additional: Option[String],
  zip: Option[String],
  city: Option[String],
  country: Option[String]

) extends AddressBase {
  def toAddress: Address = Address(UUID.randomUUID(), street, additional, zip, city, country)
}

case class Address (
  publicId: UUID,
  override street: Option[String],
  override additional: Option[String],
  override zip: Option[String],
  override city: Option[String],
  override country: Option[String]
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