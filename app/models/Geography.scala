package models

import play.api.libs.json.Json

/**
  * Created by johann on 17.11.16.
  */
case class Geography(
  country: Option[String],
  city: Option[String],
  active: Option[Boolean]
)

object Geography {
  implicit val geographyJsonFormat = Json.format[Geography]
}
