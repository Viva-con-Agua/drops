package models

import models.database.OauthClientDB
import play.api.libs.json.Json

/**
  * Created by johann on 24.11.16.
  */

case class OauthClient(id: String, secret: String, redirectUri: Option[String], grantTypes: Set[String]) {
  override def equals(o: scala.Any): Boolean = o match {
    case other : OauthClient => other.id == this.id
    case _ => false
  }

  def toOauthClientDB : OauthClientDB =
    OauthClientDB(id, secret, redirectUri, grantTypes.mkString(","))
}

object OauthClient {
  implicit val oauthClientJsonFormat = Json.format[OauthClient]
}
