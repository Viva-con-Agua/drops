package models

import java.util.UUID
import play.api.libs.json.Json

trait CityBase {
  val name: String
  val country: String
}

case class City(name: String, country: String) extends CityBase {}

object City {
  implicit val cityJsonFormat = Json.format[City]
}
