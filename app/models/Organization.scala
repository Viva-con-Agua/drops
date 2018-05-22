package models

import java.util.UUID
import play.api.libs.json.Json


trait OrganizationBase {
  val name: String
  val address: String
  val telefon: String
  val fax: String
  val email: String
  val executive: String
  val abbreviation: String
  val impressum: String
  val profile: Option[Set[Profile]]
}

case class OrganizationStub(
  name: String,
  address: String,
  telefon: String,
  fax: String,
  email: String,
  executive: String,
  abbreviation: String,
  impressum: String,
  profile: Option[Set[Profile]]

) extends OrganizationBase {
  def toOrganization: Organization = Organization(UUID.randomUUID(), name, address, telefon, fax, email, executive, abbreviation, impressum, profile)
}

case class Organization(
  publicId: UUID,
  override val name: String,
  override val address: String,
  override val telefon: String,
  override val fax: String,
  override val email: String,
  override val executive: String,
  override val abbreviation: String,
  override val impressum: String,
  override val profile: Option[Set[Profile]]
) extends OrganizationBase {
  def toOrganizationStub(): OrganizationStub =
    OrganizationStub(name, address, telefon, fax, email, executive, abbreviation, impressum, profile)
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
}
case class ProfileOrganization(
  email: String,
  publicId: UUID
  ) extends ProfileOrganizationBase

object ProfileOrganization{
  implicit val profileOrganizationJsonFormat = Json.format[ProfileOrganization]
}



