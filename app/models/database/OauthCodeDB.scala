package models.database

import java.util.UUID

import models.OauthCode
import org.joda.time.DateTime

case class OauthCodeDB (
  code: String,
  user_id: UUID,
  client_id : String,
  created: DateTime
  ){

}

object OauthCodeDB extends ((String, UUID, String, DateTime) => OauthCodeDB ){
  def apply(tuple:(String, UUID, String, DateTime)) : OauthCodeDB =
    OauthCodeDB(tuple._1, tuple._2, tuple._3, tuple._4)

  def apply(oauthCode : OauthCode) : OauthCodeDB =
    OauthCodeDB(oauthCode.code, oauthCode.user.id, oauthCode.client.id, oauthCode.created)
}