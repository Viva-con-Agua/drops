package models

import java.util.UUID
import play.api.libs.json.Json





trait BankAccountBase {
  val bankName: String
  val number: Option[String]
  val blz: Option[String]
  val iban: String
  val bic: String
}

case class BankAccount(
  bankName: String,
  number: Option[String],
  blz: Option[String],
  iban: String,
  bic: String
)extends BankAccountBase

object BankAccount {
  implicit val bankaccountJsonFormat = Json.format[BankAccount]
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
  val bankaccount: Option[Set[BankAccount]]
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
  bankaccount: Option[Set[BankAccount]],
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
  override val bankaccount: Option[Set[BankAccount]],
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
trait ProfileOrganizationNameBase {
  val email: String
  val name: String
  val role: String
}
case class ProfileOrganizationName(
  email: String,
  name: String,
  role: String
  ) extends ProfileOrganizationNameBase

object ProfileOrganizationName{
  implicit val profileOrganizationJsonFormat = Json.format[ProfileOrganizationName]
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

trait BankAccountOrganizationBase{
  val publicId: UUID
  val bankaccount: BankAccount
}

case class BankAccountOrganization(
  publicId: UUID,
  bankaccount: BankAccount
  )extends BankAccountOrganizationBase


object BankAccountOrganization{
  implicit val bankaccountOrganizationJsonFormat = Json.format[BankAccountOrganization]
}

trait BankAccountOrganizationNameBase{
  val name: String
  val bankaccount: BankAccount
}

case class BankAccountOrganizationName(
  name: String,
  bankaccount: BankAccount
  )extends BankAccountOrganizationNameBase


object BankAccountOrganizationName{
  implicit val bankaccountOrganizationJsonFormat = Json.format[BankAccountOrganizationName]
}




