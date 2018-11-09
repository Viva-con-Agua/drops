package models

import java.util.UUID

import play.api.libs.json.Json

/**
  * Created by johann on 17.11.16.
  */
 
trait CrewBase {
  val name: String
  val cities: Set[City]
}

case class CrewStub(name: String, cities: Set[City]) extends CrewBase {
  def toCrew : Crew = Crew(UUID.randomUUID(), name, cities)
}

case class Crew(
  id: UUID,
  override val name: String,
  override val cities: Set[City]
) extends CrewBase {
  override def equals(o: scala.Any): Boolean = o match {
    case other : Crew => other.name == this.name
    case _ => false
  }

  def ~(o : scala.Any) : Boolean = o == this || (o match {
    case s: Supporter => s.crew.map(_ == this).getOrElse(false)
    case _ => false
  })

  def toCrewStub() : CrewStub =
    CrewStub(name, cities)
}

object Crew {
  implicit val crewJsonFormat = Json.format[Crew]
}

object CrewStub {
  implicit val crewJsonFormat = Json.format[CrewStub]
}
