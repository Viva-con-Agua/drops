package models.database

import java.util.UUID

import org.joda.time.DateTime

case class OauthCodeDB (
  code: String,
  user_id: UUID,
  client_id : String,
  created: DateTime
  ){

}

object OauthCodeDB{
  def apply(tuple:(String, UUID, String, DateTime)) : OauthCodeDB =
    OauthCodeDB(tuple._1, tuple._2, tuple._3, tuple._4)

  def mapperTo(
              code: String, user_id: UUID, client_id: String, created: DateTime
              ) = apply(code, user_id, client_id, created)
}