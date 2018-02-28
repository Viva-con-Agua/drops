package models.database

import java.util.{Date, UUID}

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

object OauthTokenDB{
  def mapperTo(
              id: Long, token: String, refreshToken: Option[String], scope: Option[String],
              lifeSeconds: Option[Long], createdAt: Date, userId: UUID, clientId: String
              ) = apply(id, token, refreshToken, scope, lifeSeconds, createdAt, userId, clientId)
}