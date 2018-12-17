package models

import play.api.libs.json.Json

/**
  * Created by johann on 14.12.16.
  */
trait Pillar {
  def name : String

  def isKnown: Boolean = this != Unknown
}

object Pillar {

  def apply(pillar: String): Pillar = pillar match {
    case Education.name         => Education
    case Operation.name         => Operation
    case Finance.name           => Finance
    case Network.name           => Network
    case _                      => Unknown
  }

  def unapply(pillar: Pillar): Option[String] = Some(pillar.name)

  def getAll : Set[Pillar] = Set(Education, Operation, Finance, Network)

  implicit val pillarJsonFormat = Json.format[Pillar]
}

object Education extends Pillar {
  val name = "education"
}

object Operation extends Pillar {
  val name = "operation"
}

object Finance extends Pillar {
  val name = "finance"
}

object Network extends Pillar {
  val name = "network"
}

/**
  * The generic unknown pillar
  */
object Unknown extends Pillar {
  val name = "-"
}