package persistence.pool1

import play.api.libs.json.Json

case class PoolMailSwitch(mail_switch: String)

case class PoolResponseData(response: Option[PoolMailSwitch], context: String, code: Int, message: String)

object PoolResponseData {
  implicit val poolMailSwitchFormat = Json.format[PoolMailSwitch]
  implicit val poolResponseDataFormat = Json.format[PoolResponseData]
}
