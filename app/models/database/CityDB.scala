package models.database

case class CityDB(
                   id: Long,
                   name: String,
                   crewId: Long
                 )

object CityDB extends ((Long, String, Long) => CityDB ){

}
