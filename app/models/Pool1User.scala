package models

case class Pool1User (
  email: String,
  confirmed: Boolean
)

object Pool1User {
  import play.api.libs.json.Json

  implicit val pool1UserFormat = Json.format[Pool1User]
}
