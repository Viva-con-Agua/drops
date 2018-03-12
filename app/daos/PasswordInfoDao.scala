package daos

import scala.concurrent.Future
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection
import models.{Profile, User}
import User._
import daos.schema.{LoginInfoTableDef, PasswordInfoTableDef, ProfileTableDef}
import models.database.PasswordInfoDB
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._

trait PasswordInfoDao extends DelegableAuthInfoDAO[PasswordInfo] {
  def find(loginInfo: LoginInfo) : Future[Option[PasswordInfo]]
  def add(loginInfo:LoginInfo, authInfo:PasswordInfo):Future[PasswordInfo]
  def update(loginInfo:LoginInfo, authInfo:PasswordInfo):Future[PasswordInfo]
  def save(loginInfo:LoginInfo, authInfo:PasswordInfo):Future[PasswordInfo]
  def remove(loginInfo:LoginInfo):Future[Unit]
}

class MongoPasswordInfoDao extends PasswordInfoDao {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  val users = reactiveMongoApi.db.collection[JSONCollection]("users")

  def find(loginInfo:LoginInfo):Future[Option[PasswordInfo]] = for {
    user <- users.find(Json.obj(
      "profiles.loginInfo" -> loginInfo
    )).one[User]
  } yield user.flatMap(_.profiles.find(_.loginInfo == loginInfo)).flatMap(_.passwordInfo)

  def add(loginInfo:LoginInfo, authInfo:PasswordInfo):Future[PasswordInfo] = 
    users.update(Json.obj(
      "profiles.loginInfo" -> loginInfo
    ), Json.obj(
      "$set" -> Json.obj("profiles.$.passwordInfo" -> authInfo)
    )).map(_ => authInfo)

  def update(loginInfo:LoginInfo, authInfo:PasswordInfo):Future[PasswordInfo] = 
    add(loginInfo, authInfo)

  def save(loginInfo:LoginInfo, authInfo:PasswordInfo):Future[PasswordInfo] = 
    add(loginInfo, authInfo)

  def remove(loginInfo:LoginInfo):Future[Unit] = 
    users.update(Json.obj(
      "profiles.loginInfo" -> loginInfo
    ), Json.obj(
      "$pull" -> Json.obj(
        "profiles" -> Json.obj("loginInfo" -> loginInfo)
      )
    )).map(_ => ())
}


class MariadbPasswordInfoDao extends PasswordInfoDao{
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  val passwordInfos = TableQuery[PasswordInfoTableDef]
  val loginInfos = TableQuery[LoginInfoTableDef]
  val profiles = TableQuery[ProfileTableDef]

  override def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = {
    val action = for{
      ((_, passwordInfo), _) <- (profiles
        join passwordInfos on (_.id === _.profileId) //user.id === profile.userId
        join loginInfos.filter(lI => lI.providerId === loginInfo.providerID && lI.providerKey === loginInfo.providerKey)
        on (_._1.id === _.profileId) //profiles.id === loginInfo.profileId
        )} yield(passwordInfo)
    
    dbConfig.db.run(action.result).map(r => r.headOption.map(pInfo => PasswordInfo(pInfo.hasher, pInfo.password)))
  }

  def find(id: Long) : Future[PasswordInfo] = {
    dbConfig.db.run(passwordInfos.filter(_.id === id).result).map(pInfo => PasswordInfo(pInfo.head.hasher, pInfo.head.password))
  }

  override def add(loginInfo: LoginInfo, authInfo: PasswordInfo) :Future[PasswordInfo] = {
    remove(loginInfo).flatMap(_ => {
      dbConfig.db.run(loginInfos.filter(lI => lI.providerId === loginInfo.providerID && lI.providerKey === loginInfo.providerKey).result).flatMap(lInfo => {
        val profileId = lInfo.head.profileId
        dbConfig.db.run(passwordInfos += PasswordInfoDB(0, authInfo, profileId)).flatMap(id => find(id))
      })
    })
  }

  override def update(loginInfo: LoginInfo, authInfo: PasswordInfo):Future[PasswordInfo] = add(loginInfo, authInfo)

  override def save(loginInfo: LoginInfo, authInfo: PasswordInfo) :Future[PasswordInfo] = add(loginInfo, authInfo)

  override def remove(loginInfo: LoginInfo) : Future[Unit] = {
    val profileId = loginInfos.filter(lI => lI.providerId === loginInfo.providerID && lI.providerKey === loginInfo.providerKey)
    val deletePasswordInfo = passwordInfos.filter(_.profileId.in(profileId.map(_.profileId)))

    dbConfig.db.run((profileId.result andThen deletePasswordInfo.delete).transactionally).map(_ => ())
  }
}