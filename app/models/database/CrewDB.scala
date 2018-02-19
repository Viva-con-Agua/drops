package models.database

import java.util.UUID

import models.Crew

case class CrewDB(
                 id: Long,
                 publicId: UUID,
                 name: String,
                 country: String
               ) {
  def toCrew(cities : Set[String]) : Crew = Crew(publicId, name, country, cities)

}

object CrewDB{
  def mapperTo(
              id: Long, publicId: UUID, name: String, country: String
              ) = apply(id, publicId, name, country)

  def apply (crew: Crew): CrewDB = CrewDB(0, crew.id, crew.name, crew.country)
}


