package api.query

import play.api.libs.json.Json

trait Area {
  val name : String
}

object Area {
  def apply(name : String) = name match {
    case RoleArea.name => RoleArea
    case PillarArea.name => PillarArea
    case _ => throw new Exception // TODO: Use meaningful exception!
  }
  def unapply(area: Area): Option[String] = Some(area.name)

  implicit val areaJsonFormat = Json.format[Area]
}

object RoleArea extends Area {
  val name = "role"
}

object PillarArea extends Area {
  val name = "pillar"
}