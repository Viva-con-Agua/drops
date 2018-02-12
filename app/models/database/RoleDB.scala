package models.database

case class RoleDB (
  id: Long,
  name: String
  )

object RoleDB{
  def apply(tuple: (Long, String)): RoleDB =
    RoleDB(tuple._1, tuple._2)

  def mapperTo(id: Long, name: String) = apply(id, name)
}
