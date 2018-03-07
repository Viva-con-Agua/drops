package models.database

import java.util.{Date, UUID}

import models.OauthToken

case class OauthTokenDB (
    id: Long,
    token: String,
    refreshToken: Option[String],
    scope: Option[String],
    lifeSeconds: Option[Long],
    createdAt: Date,
    userId: UUID,
    clientId: String
  )

object OauthTokenDB extends ((Long, String, Option[String], Option[String], Option[Long], Date, UUID, String) => OauthTokenDB ){
  def apply(oauthToken: OauthToken) : OauthTokenDB =
    OauthTokenDB(
      0, oauthToken.accessToken.token, oauthToken.accessToken.refreshToken, oauthToken.accessToken.scope, oauthToken.accessToken.lifeSeconds,
      oauthToken.accessToken.createdAt, oauthToken.userId, oauthToken.client.id
    )

  def apply(oauthToken: OauthToken, id: Long) : OauthTokenDB =
    OauthTokenDB(
      id, oauthToken.accessToken.token, oauthToken.accessToken.refreshToken, oauthToken.accessToken.scope, oauthToken.accessToken.lifeSeconds,
      oauthToken.accessToken.createdAt, oauthToken.userId, oauthToken.client.id
    )
}