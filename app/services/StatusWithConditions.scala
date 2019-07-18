package services

import play.api.libs.json._

case class Conditions(hasAddress: Option[Boolean], hasPrimaryCrew: Option[Boolean], isActive: Option[Boolean])
case class StatusWithConditions(status: String, conditions: Conditions)

object StatusWithConditions {
  implicit val conditionsFormat = Json.format[Conditions]
  implicit val statusConditionsFormat = Json.format[StatusWithConditions]
}