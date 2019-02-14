package daos

import scala.concurrent.Future
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
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
   /* TODO: implement PasswordInfo with Option   
      pwInfoOption match {
      case Some(pInfo) => Some(PasswordInfo(pInfo.hasher, pInfo.password))
      case _ => None
    })*/
  
  override def add(loginInfo: LoginInfo, authInfo: PasswordInfo) :Future[PasswordInfo] = {
    remove(loginInfo).flatMap(_ => {
      dbConfig.db.run(loginInfos.filter(lI => lI.providerId === loginInfo.providerID && lI.providerKey === loginInfo.providerKey).result).flatMap(lInfo => {
        val profileId = lInfo.head.profileId
        dbConfig.db.run((passwordInfos returning passwordInfos.map(_.id)) += PasswordInfoDB(0, authInfo, profileId)).flatMap(id => find(id))
      })
    })
  }

  override def update(loginInfo: LoginInfo, passwordInfo: PasswordInfo):Future[PasswordInfo] = {
    // action for get the PasswordInfoDB
    val action = for {
      // the frontend identifire is LoginInfo l
      l <- loginInfos.filter(entry => entry.providerId === loginInfo.providerID && entry.providerKey === loginInfo.providerKey)
      // the passwordInfo profileId is the same as l.profileId. We can't update here with slick because we can't accress the id in .update()
      pw <- passwordInfos.filter(entry => entry.profileId === l.profileId)
    } yield pw
    // run action an get the result. pInfo contains the PasswordInfoDB
    dbConfig.db.run(action.result).flatMap(pInfo =>{
      // update the new PasswordInfo with same id's
      dbConfig.db.run((passwordInfos.filter(_.id === pInfo.head.id).update(PasswordInfoDB(pInfo.head.id, passwordInfo, pInfo.head.profileId))))
      // get id and find the new PasswordInfo
    }).flatMap((id) => find(id))

  }

  override def save(loginInfo: LoginInfo, authInfo: PasswordInfo) :Future[PasswordInfo] = {
    // find profile id via loginInfo
    dbConfig.db.run(loginInfos.filter(lI => lI.providerId === loginInfo.providerID && lI.providerKey === loginInfo.providerKey).result)
      .flatMap(lInfo => {
        // save PasswordInfo with new id and profile id
        dbConfig.db.run((passwordInfos returning passwordInfos.map(_.id)) += PasswordInfoDB(0, authInfo, lInfo.head.profileId))
        // find PasswordInfo via returned id
          .flatMap(id => find(id))
      })
  }

  override def remove(loginInfo: LoginInfo) : Future[Unit] = {
    val profileId = loginInfos.filter(lI => lI.providerId === loginInfo.providerID && lI.providerKey === loginInfo.providerKey)
    val deletePasswordInfo = passwordInfos.filter(_.profileId.in(profileId.map(_.profileId)))

    dbConfig.db.run((profileId.result andThen deletePasswordInfo.delete).transactionally).map(_ => ())
  }
}
