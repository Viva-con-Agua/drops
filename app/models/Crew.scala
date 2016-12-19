package models

import play.api.libs.json.Json

/**
  * Created by johann on 17.11.16.
  */
case class Crew(
  name: String,
  country: String,
  cities: Set[String]
)

case class CrewSupporter(
  crew: Crew,
  active: Boolean
)

object Crew {
  implicit val crewJsonFormat = Json.format[Crew]
}


object CrewSupporter {
  implicit val crewSupporterJsonFormat = Json.format[CrewSupporter]
}
