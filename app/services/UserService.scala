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
import daos.{AccessRightDao, TaskDao, UserDao}
import models.AccessRight
import models.{Profile, ProfileImage, User}
import play.api.Logger
import utils.Nats

class UserService @Inject() (userDao:UserDao, taskDao: TaskDao, accessRightDao: AccessRightDao, nats: Nats) extends IdentityService[User] {
  val logger: Logger = Logger(this.getClass())
  def retrieve(loginInfo:LoginInfo):Future[Option[User]] = userDao.find(loginInfo)
  def save(user:User) = userDao.save(user)
  def saveImage(profile: Profile, avatar: ProfileImage) = userDao.saveProfileImage(profile, avatar)
  def update(updatedUser: User) = {
    nats.publishUpdate("USER", updatedUser.id) 
    userDao.replace(updatedUser)
  }

  def updateSupporter(id: UUID, profile: Profile) = {
    //nats.publishUpdate("USER", id)
    userDao.updateSupporter(profile)
  }
  def find(id:UUID) = userDao.find(id)
  def confirm(loginInfo:LoginInfo) = userDao.confirm(loginInfo)
  def link(user:User, socialProfile:CommonSocialProfile) = {
    val profile = Profile(socialProfile)
    if (user.profiles.exists(_.loginInfo == profile.loginInfo)) Future.successful(user) else userDao.link(user, profile)
  }

  def save(socialProfile:CommonSocialProfile) = {
    val profile = Profile(socialProfile)
    userDao.find(profile.loginInfo).flatMap {
      case None => userDao.save(User(UUID.randomUUID(), List(profile), System.currentTimeMillis(), System.currentTimeMillis()))
      case Some(user) => userDao.update(profile)
    }
  }

  def list = userDao.list
  def listOfStubs = userDao.listOfStubs

  def accessRights(userId: UUID) : Future[Seq[AccessRight]] = {
    taskDao.idsForUser(userId).flatMap(taskIds => accessRightDao.forTaskList(taskIds))
  }
  //todo
  def delete(userId: UUID) = {
    nats.publishDelete("USER", userId)
  }
  
  def getProfile(email: String) = userDao.getProfile(email)
  def profileListByRole(id: UUID, role: String) = userDao.profileListByRole(id, role)
  def profileByRole(id: UUID, role: String) = userDao.profileByRole(id, role)

  def assign(crewUUID: UUID, user: User) = user.profiles.headOption match {
    case Some(profile) => userDao.setCrew(crewUUID, profile)
    case _ => Future.successful(Right("service.user.error.notFound.profile"))
  }

  /**
    * Removes a crew from a user.
    * @author Johann Sell
    * @param user
    */
  def deAssign(user: User) = user.profiles.headOption match {
    case Some(profile) => profile.supporter.crew match {
      case Some(crew) => userDao.removeCrew(crew.id, profile)
      case None => Future.successful(Right("service.user.error.notFound.crew"))
    }
    case None => Future.successful(Right("service.user.error.notFound.profile"))
  }

  def assignOnlyOne(crewUUID: UUID, user: User) = user.profiles.headOption match {
    case Some(profile) if profile.supporter.crew.isDefined => this.deAssign(user).flatMap(_ match {
      case Left(i) if i > 0 => this.assign(crewUUID, user)
      case Left(i) => Future.successful(Right("service.user.error.oldCrewNotDeleted"))
      case Right(x) => Future.successful( Right( x ) )
    })
    case Some(profile) => this.assign(crewUUID, user)
    case None => Future.successful(Right("service.user.error.notFound.profile"))
  }
}
