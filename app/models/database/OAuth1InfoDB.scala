package models.database

import com.mohiva.play.silhouette.impl.providers.OAuth1Info

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

}



