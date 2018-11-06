package models.database

import java.util.UUID

import models.{Crew, City}

case class CrewDB(
                 id: Long,
                 publicId: UUID,
                 name: String
               ) {
  def toCrew(cities : Set[City]) : Crew = Crew(publicId, name, cities)

}

object CrewDB extends((Long, UUID, String) => CrewDB ){
  def apply (crew: Crew): CrewDB = CrewDB(0, crew.id, crew.name)
}
