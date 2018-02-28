package models

import java.util.{Date, UUID}

import models.database.OauthTokenDB
import play.api.libs.Crypto
import play.api.libs.json.Json

import scalaoauth2.provider.{AccessToken, AuthInfo}

/**
  * Created by johann on 25.11.16.
  */
case class OauthToken(accessToken: AccessToken, client: OauthClient, userId: UUID){
  def toOauthTokenDB : OauthTokenDB = OauthTokenDB(
    0, accessToken.token, accessToken.refreshToken, accessToken.scope, accessToken.lifeSeconds,
    accessToken.createdAt, userId, client.id
  )

  def toOauthTokenDBWithId(id : Long) = OauthTokenDB(
    id, accessToken.token, accessToken.refreshToken, accessToken.scope, accessToken.lifeSeconds,
    accessToken.createdAt, userId, client.id
  )
}

object OauthToken {

  def createAccessToken(authInfo: AuthInfo[User], token: Option[String] = None) : AccessToken = {
    val accessTokenExpiresIn = Some(60L * 60L) // 1 hour
    val refreshToken = Some(Crypto.generateToken)
    val accessToken = token.getOrElse(Crypto.generateToken)
    val now = new Date()

    AccessToken(accessToken, refreshToken, authInfo.scope, accessTokenExpiresIn, now)
  }

  implicit val accessTokenJsonFormat = Json.format[AccessToken]
  implicit val oauthTokenJsonFormat = Json.format[OauthToken]
}
