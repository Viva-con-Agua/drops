package models

import play.api.libs.json.Json

/**
  * Created by johann on 17.11.16.
  */
case class Crew(
  name: String,
  country: String,
  cities: Set[String]
) {
  override def equals(o: scala.Any): Boolean = o match {
    case other : Crew => other.name == this.name
    case _ => false
  }

  def ~(o : scala.Any) : Boolean = o == this || (o match {
    case cs: CrewSupporter => cs.crew == this
    case _ => false
  })
}

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
