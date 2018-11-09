package models.converter

import models.{OauthClient}
import models.database.{OauthClientDB}

object OauthClientConverter {

  def buildListFromResult(result : Seq[(OauthClientDB)]) : List[OauthClient] = {
    val oauthClientList = result.seq.foldLeft[List[OauthClient]](Nil)((oauthClientList, dbEntry) => {
      val oauthClient = dbEntry
      oauthClientList ++ List(oauthClient.toOauthClient)
    })
    oauthClientList
  }
}
