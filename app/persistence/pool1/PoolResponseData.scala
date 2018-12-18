package persistence.pool1

import play.api.libs.json.Json

case class PoolMailSwitch(mail_switch: String)

case class PoolResponseData(status: Int, message: String, data: Option[PoolMailSwitch])

object PoolResponseData {
  implicit val poolMailSwitchFormat = Json.format[PoolMailSwitch]
  implicit val poolResponseDataFormat = Json.format[PoolResponseData]
}
