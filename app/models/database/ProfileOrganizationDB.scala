package models.database

case class ProfileOrganizationDB(
  profileId: Long,
  organizationId: Long,
  role: String
)
object ProfileOrganizationDB {
  

  def mapperTo(
    profileId: Long, organizationId: Long, role: String) = apply(profileId, organizationId, role)
}
