package models.database

case class CityDB(
                   id: Long,
                   name: String,
                   crewId: Long
                 )

object CityDB{
  def mapperTo(
              id: Long, name: String, crewId:Long
              ) = apply(id, name, crewId)
}
