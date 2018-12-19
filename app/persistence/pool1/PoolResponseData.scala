package persistence.pool1

import play.api.libs.json.Json

case class PoolMailSwitch(mail_switch: String)

case class PoolResponseData(code: Int, message: String, response: Option[PoolMailSwitch])

object PoolResponseData {
  implicit val poolMailSwitchFormat = Json.format[PoolMailSwitch]
  implicit val poolResponseDataFormat = Json.format[PoolResponseData]
}
