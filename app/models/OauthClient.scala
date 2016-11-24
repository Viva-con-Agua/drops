package models

import play.api.libs.json.Json

/**
  * Created by johann on 24.11.16.
  */

case class OauthClient(id: String, secret: String, redirectUri: String, grantTypes: Set[String])

object OauthClient {
  implicit val oauthClientJsonFormat = Json.format[OauthClient]
}
