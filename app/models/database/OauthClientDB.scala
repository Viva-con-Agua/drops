package models.database

import models.OauthClient

case class OauthClientDB (
                           id: String,
                           secret: String,
                           redirectUri: Option[String],
                           grantTypes: String
                         ){
  def toOauthClient : OauthClient = {
    OauthClient(id, secret, redirectUri, grantTypes.split(",").toSet)
  }
}

object OauthClientDB{
  def apply(tuple: (String, String, Option[String], String)) : OauthClientDB =
    OauthClientDB(tuple._1, tuple._2, tuple._3, tuple._4)

  def mapperTo(
              id: String, secret: String, redirectUri: Option[String], grantTypes: String
              ) = apply(id, secret, redirectUri, grantTypes)
}


