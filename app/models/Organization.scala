package models

import java.util.UUID
import play.api.libs.json.Json





trait BankaccountBase {
  val bankName: String
  val number: Option[String]
  val blz: Option[String]
  val iban: String
  val bic: String
}

case class Bankaccount(
  bankName: String,
  number: Option[String],
  blz: Option[String],
  iban: String,
  bic: String
)extends BankaccountBase

object Bankaccount {
  implicit val bankaccountJsonFormat = Json.format[Bankaccount]
}

trait OrganizationBase {
  val name: String
  val address: String
  val telefon: String
  val fax: String
  val email: String
  val typ: String
  val executive: String
  val abbreviation: String
  val impressum: Boolean
  val bankaccount: Option[Set[Bankaccount]]
  val profile: Option[Set[String]]
}

case class OrganizationStub(
  name: String,
  address: String,
  telefon: String,
  fax: String,
  email: String,
  typ: String,
  executive: String,
  abbreviation: String,
  impressum: Boolean,
  bankaccount: Option[Set[Bankaccount]],
  profile: Option[Set[String]]

) extends OrganizationBase {
  def toOrganization: Organization = Organization(UUID.randomUUID(), name, address, telefon, fax, email, typ, executive, abbreviation, impressum, bankaccount, profile)
}

case class Organization(
  publicId: UUID,
  override val name: String,
  override val address: String,
  override val telefon: String,
  override val fax: String,
  override val email: String,
  override val typ: String,
  override val executive: String,
  override val abbreviation: String,
  override val impressum: Boolean,
  override val bankaccount: Option[Set[Bankaccount]],
  override val profile: Option[Set[String]]
) extends OrganizationBase {
  def toOrganizationStub(): OrganizationStub =
    OrganizationStub(name, address, telefon, fax, email, typ, executive, abbreviation, impressum, bankaccount, profile)
}



object Organization {
  implicit val organizationJsonFormat = Json.format[Organization]
}

object OrganizationStub {
  implicit val organizationJsonFormat = Json.format[OrganizationStub]
}

trait OrganizationUUIDBase {
  val publicId: UUID
}

case class OrganizationUUID(
  publicId: UUID
)extends OrganizationUUIDBase

object OrganizationUUID{
  implicit val organizationUUIDJsonFormat = Json.format[OrganizationUUID]
}

trait ProfileOrganizationBase {
  val email: String
  val publicId: UUID
  val role: String
}
case class ProfileOrganization(
  email: String,
  publicId: UUID,
  role: String
  ) extends ProfileOrganizationBase

object ProfileOrganization{
  implicit val profileOrganizationJsonFormat = Json.format[ProfileOrganization]
}

trait BankaccountOrganizationBase{
  val publicId: UUID
  val bankaccount: Bankaccount
}

case class BankaccountOrganization(
  publicId: UUID,
  bankaccount: Bankaccount
  )extends BankaccountOrganizationBase


object BankaccountOrganization{
  implicit val bankaccountOrganizationJsonFormat = Json.format[BankaccountOrganization]
}




