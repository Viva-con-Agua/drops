package daos

import scala.concurrent.Future
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth1Info
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import models.User
import User._
import daos.schema.{LoginInfoTableDef, OAuth1InfoTableDef, ProfileTableDef}
import models.database.OAuth1InfoDB
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.lifted.TableQuery
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._

trait OAuth1InfoDao extends DelegableAuthInfoDAO[OAuth1Info] {
  def find(loginInfo:LoginInfo):Future[Option[OAuth1Info]]
  def add(loginInfo:LoginInfo, authInfo:OAuth1Info):Future[OAuth1Info]
  def update(loginInfo:LoginInfo, authInfo:OAuth1Info):Future[OAuth1Info]
  def save(loginInfo:LoginInfo, authInfo:OAuth1Info):Future[OAuth1Info]
  def remove(loginInfo:LoginInfo):Future[Unit]
}

class MariadbOAuth1InfoDao extends OAuth1InfoDao{
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  val oAuth1Infos = TableQuery[OAuth1InfoTableDef]
  val loginInfos = TableQuery[LoginInfoTableDef]
  val profiles = TableQuery[ProfileTableDef]

  override def find(loginInfo: LoginInfo): Future[Option[OAuth1Info]] = {
        val action = for{
      ((_, oAuth1Info), _) <- (profiles
        join oAuth1Infos on (_.id === _.profileId) //user.id === profile.userId
        join loginInfos.filter(lI => lI.providerId === loginInfo.providerID && lI.providerKey === loginInfo.providerKey)
        on (_._1.id === _.profileId) //profiles.id === loginInfo.profileId
        )} yield(oAuth1Info)

    dbConfig.db.run(action.result).map(r => r.headOption.map(oaInfo => OAuth1Info(oaInfo.token, oaInfo.secret)))
  }

  def find(id: Long) : Future[OAuth1Info] = {
    dbConfig.db.run(oAuth1Infos.filter(_.id === id).result).map(oaInfo => OAuth1Info(oaInfo.head.secret, oaInfo.head.token))
  }

  override def add(loginInfo: LoginInfo, authInfo: OAuth1Info): Future[OAuth1Info] = {
    remove(loginInfo).flatMap(_ => {
      dbConfig.db.run(loginInfos.filter(lI => lI.providerId === loginInfo.providerID && lI.providerKey === loginInfo.providerKey).result).flatMap(lInfo => {
        val profileId = lInfo.head.profileId
        dbConfig.db.run(oAuth1Infos += OAuth1InfoDB(authInfo, profileId)).flatMap(id => find(id))
      })
    })

    Future(authInfo)
  }

  override def update(loginInfo: LoginInfo, authInfo: OAuth1Info): Future[OAuth1Info] =
    add(loginInfo, authInfo)

  override def save(loginInfo: LoginInfo, authInfo: OAuth1Info): Future[OAuth1Info] =
    add(loginInfo, authInfo)

  override def remove(loginInfo: LoginInfo): Future[Unit] = {
    val profileId = loginInfos.filter(lI => lI.providerId === loginInfo.providerID && lI.providerKey === loginInfo.providerKey)
    val deleteOAuth1Info = oAuth1Infos.filter(_.profileId.in(profileId.map(_.profileId)))

    dbConfig.db.run((profileId.result andThen deleteOAuth1Info.delete).transactionally).map(_ => ())
  }
}
