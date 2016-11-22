package models

import play.api.libs.json.Json

/**
  * Created by johann on 17.11.16.
  */
case class Crew(
  country: String,
  city: String,
  active: Boolean
)

object Crew {
  implicit val crewJsonFormat = Json.format[Crew]
}
