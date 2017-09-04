package models

import java.util.UUID

import play.api.libs.json.Json

/**
  * Created by johann on 17.11.16.
  */

trait CrewBase {
  val name: String
  val country: String
  val cities: Set[String]
}

case class CrewStub(name: String, country: String, cities: Set[String]) extends CrewBase {
  def toCrew : Crew = Crew(UUID.randomUUID(), name, country, cities)
}

case class Crew(
  id: UUID,
  override val name: String,
  override val country: String,
  override val cities: Set[String]
) extends CrewBase {
  override def equals(o: scala.Any): Boolean = o match {
    case other : Crew => other.name == this.name
    case _ => false
  }

  def ~(o : scala.Any) : Boolean = o == this || (o match {
    case s: Supporter => s.crew == this
    case _ => false
  })
}

object Crew {
  implicit val crewJsonFormat = Json.format[Crew]
}

object CrewStub {
  implicit val crewJsonFormat = Json.format[CrewStub]
}

