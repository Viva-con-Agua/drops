package models.database

import java.util.UUID

case class CrewDB(
                 id: Long,
                 publicId: UUID,
                 name: String,
                 country: String
               )

object CrewDB{
  def mapperTo(
              id: Long, publicId: UUID, name: String, country: String
              ) = apply(id, publicId, name, country)
}


