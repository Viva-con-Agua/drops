package models.database

import models.City

case class CityDB(
                   id: Long,
                   name: String,
                   country: String,
                   crewId: Long
                 ) {
  def toCity : City = City(this.name, this.country)
                 }

object CityDB extends ((Long, String, String, Long) => CityDB ){

}
