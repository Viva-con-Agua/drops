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

object OauthClientDB extends ((String, String, Option[String], String) => OauthClientDB ){
  def apply(tuple: (String, String, Option[String], String)) : OauthClientDB =
    OauthClientDB(tuple._1, tuple._2, tuple._3, tuple._4)

  def apply (oauthClient : OauthClient) : OauthClientDB =
    OauthClientDB(oauthClient.id, oauthClient.secret, oauthClient.redirectUri, oauthClient.grantTypes.mkString(","))

}


