package models.database

case class ProfileOrganizationDB(
  profileId: Long,
  organizationId: Long
)
object ProfileOrganizationDB {
  

  def mapperTo(
    profileId: Long, organizationId: Long) = apply(profileId, organizationId)
}
