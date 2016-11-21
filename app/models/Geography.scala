package models

import play.api.libs.json.Json

/**
  * Created by johann on 17.11.16.
  */
case class Geography(
  country: String,
  city: String,
  active: Boolean
)

object Geography {
  implicit val geographyJsonFormat = Json.format[Geography]
}
