package models.database

import models.Pool1User

case class Pool1UserDB (
                    id: Long,
                    email: String,
                    confirmed: Boolean
                  ){
    def toPool1User: Pool1User = Pool1User(email, confirmed);
}

object Pool1UserDB extends((Long, String, Boolean) => Pool1UserDB ){
  def apply (pool1User: Pool1User): Pool1UserDB = Pool1UserDB(0, pool1User.email, pool1User.confirmed)
}


