package services

import play.api.libs.json._

/** Case class condition
  * Contains the pre conditions for the supporter to change the nvm state to 'active'
  * @param hasAddress Does the supporter have an address
  * @param hasPrimaryCrew Does the supporter have a primary crew selected
  * @param isActive Is the supporter active in his primary crew
  */
case class Conditions(hasAddress: Option[Boolean], hasPrimaryCrew: Option[Boolean], isActive: Option[Boolean])

/** Combines the current state of the non voting membership and the precondictions
  * @param status
  * @param conditions
  */
case class StatusWithConditions(status: String, conditions: Conditions)

object StatusWithConditions {
  implicit val conditionsFormat = Json.format[Conditions]
  implicit val statusConditionsFormat = Json.format[StatusWithConditions]
}