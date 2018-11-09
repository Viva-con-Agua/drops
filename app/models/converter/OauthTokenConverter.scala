package models.converter

import models.OauthToken
import models.database.{OauthClientDB, OauthTokenDB, UserDB}

import scalaoauth2.provider.AccessToken

object OauthTokenConverter{
  def buildOauthTokenObjectFromResult(result : Seq[(OauthTokenDB, OauthClientDB, UserDB)]) : Option[OauthToken] = {
    result.headOption.map(r => {
      val oauthToken = r._1
      OauthToken(AccessToken(oauthToken.token, oauthToken.refreshToken, oauthToken.scope, oauthToken.lifeSeconds, oauthToken.createdAt), result.head._2.toOauthClient, result.head._3.publicId)
    })
  }
}
