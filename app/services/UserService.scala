package services

import java.util.UUID
import javax.inject._

import scala.concurrent.Future

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.collection.JSONCollection

import daos.UserDao
import models.{User,Profile}

class UserService @Inject() (userDao:UserDao) extends IdentityService[User] {
  def retrieve(loginInfo:LoginInfo):Future[Option[User]] = userDao.find(loginInfo)
  def save(user:User) = userDao.save(user)
  def update(updatedUser: User) = userDao.replace(updatedUser)
  def find(id:UUID) = userDao.find(id)
  def confirm(loginInfo:LoginInfo) = userDao.confirm(loginInfo)
  def link(user:User, socialProfile:CommonSocialProfile) = {
    val profile = Profile(socialProfile)
    if (user.profiles.exists(_.loginInfo == profile.loginInfo)) Future.successful(user) else userDao.link(user, profile)
  }

  def save(socialProfile:CommonSocialProfile) = {
    val profile = Profile(socialProfile)
    userDao.find(profile.loginInfo).flatMap {
      case None => userDao.save(User(UUID.randomUUID(), List(profile)))
      case Some(user) => userDao.update(profile)
    }
  }
}
