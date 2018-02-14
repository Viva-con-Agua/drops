package models.database

import com.mohiva.play.silhouette.impl.providers.OAuth1Info
import play.api.libs.json.{JsPath, Reads, _}
import play.api.libs.functional.syntax._


case class OAuth1InfoDB  (
                      id: Long,
                      token: String,
                      secret: String,
                      profileId: Long
                    )

object OAuth1InfoDB{
  def apply(authInfo: OAuth1Info, profileId: Long): OAuth1InfoDB =
    OAuth1InfoDB(0, authInfo.token, authInfo.secret, profileId)

  def apply(tuple: (Long, String, String, Long)): OAuth1InfoDB =
    OAuth1InfoDB(tuple._1, tuple._2, tuple._3, tuple._4)

  def mapperTo(
                id: Long, token: String, secret: String, profileId: Long
              ) = apply(id, token, secret, profileId)

  implicit val oAuth1InfoDBWrites : OWrites[OAuth1InfoDB] = (
    (JsPath \ "id").write[Long]and
      (JsPath \ "token").write[String] and
      (JsPath \ "secret").write[String] and
      (JsPath \ "profileId").write[Long]
  )(unlift(OAuth1InfoDB.unapply))

  implicit val oAuth1InfoDBReads : Reads[OAuth1InfoDB] = (
    (JsPath \ "id").read[Long]and
      (JsPath \ "token").read[String] and
      (JsPath \ "secret").read[String] and
      (JsPath \ "profileId").read[Long]
  ).tupled.map((oAuth1InfoDB) => OAuth1InfoDB(oAuth1InfoDB))
}



