package services

import java.util.UUID

import javax.inject._

import scala.concurrent.Future
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import daos.{AccessRightDao, CrewDao, TaskDao, UserDao}
import models._
import persistence.pool1.PoolService
import play.api.Logger
import utils.Nats

class UserService @Inject() (
                              userDao:UserDao,
                              poolService: PoolService,
                              pool1DBService: Pool1Service,
                              taskDao: TaskDao,
                              crewDao: CrewDao,
                              accessRightDao: AccessRightDao,
                              nats: Nats
                            ) extends IdentityService[User] {
  val logger: Logger = Logger(this.getClass())

  def retrieve(loginInfo:LoginInfo):Future[Option[User]] = userDao.find(loginInfo)
  def save(user:User) = {
    userDao.save(user).map(user => {
      //nats.publishCreate("USER", user.id)
      user
    })
  }
  //def saveImage(profile: Profile, avatar: ProfileImage) = userDao.saveProfileImage(profile, avatar)

  def update(updatedUser: User) = {
    userDao.replace(updatedUser).map(user => {
      nats.publishUpdate("USER", updatedUser.id)
      poolService.update(user) // Todo: Consider result?!
      user
    })
  }

  def updateSupporter(id: UUID, profile: Profile) = {
    //nats.publishUpdate("USER", id)
    userDao.updateSupporter(profile).map(profileOpt => {
      nats.publishUpdate("USER", id)
      if(profileOpt.isDefined) {
        userDao.find(id).map(_.map(user => poolService.update(user))) // Todo: Consider result?!
      }
      profileOpt
    })
  }
  def find(id:UUID) = userDao.find(id)
  def findUnconfirmedPool1User(email: String) = pool1DBService.pool1user(email).map(_ match {
    case Some(pool1user) if !pool1user.confirmed => Some(pool1user)
    case _ => None
  })

  def logout(user: User) : Future[Boolean] = {
    poolService.activated match {
      case true => poolService.logout(user).map(success => {
        nats.publishLogout(user.id)
        success
      })
      case false => {
        nats.publishLogout(user.id)
        Future.successful(true)
      }
    }
  }

  def confirm(loginInfo:LoginInfo) = {
    userDao.confirm(loginInfo).map(user => {
      // Save the user in Pool 1 AFTER confirm
      poolService.save(user) // Todo: Consider result?!
    })
  }
  def confirmPool1User(email: String) = pool1DBService.confirmed(email)

  def link(user:User, socialProfile:CommonSocialProfile) = {
    val profile = Profile(socialProfile)
    if (user.profiles.exists(_.loginInfo == profile.loginInfo)) Future.successful(user) else userDao.link(user, profile)
  }

  def getNewsletterPool1Settings(id: UUID) = poolService.read(id).map(_.map(_.mail_switch))
  def setNewsletterPool1Settings(user: User, mailSwitch: String) = {
    val allowedValues = Seq("all", "none", "regional", "global")
    allowedValues.contains(mailSwitch) match {
      case true => poolService.update(user, Some(mailSwitch))
      case false => Future.successful(false)
    }
  }

  def save(socialProfile:CommonSocialProfile) = {
    def onSuccess(user: User, create: Boolean) = {
      if(create) {
        nats.publishCreate("USER", user.id)
        poolService.save(user) // Todo: Consider result?!
      } else {
        nats.publishUpdate("USER", user.id)
        poolService.update(user) // Todo: Consider result?!
      }
      user
    }
    val profile = Profile(socialProfile)
    userDao.find(profile.loginInfo).flatMap {
      case None => userDao.save(User(UUID.randomUUID(), List(profile), System.currentTimeMillis(), System.currentTimeMillis()))
        .map(onSuccess(_, true))
      case Some(user) => userDao.update(profile)
        .map(onSuccess(_, false))
    }
  }

  def list = userDao.list
  def listOfStubs = userDao.listOfStubs

  def accessRights(userId: UUID) : Future[Seq[AccessRight]] = {
    taskDao.idsForUser(userId).flatMap(taskIds => accessRightDao.forTaskList(taskIds))
  }
  
  def delete(userId: UUID): Future[Boolean] = {
    userDao.delete(userId).map(_ match {
      case true => {
        nats.publishDelete("USER", userId) 
        poolService.delete(userId)
        true
      }
      case false => false
    })
  }

  def updateProfileEmail(email: String, profile: Profile) = userDao.updateProfileEmail(email, profile)
  def getProfile(email: String) = userDao.getProfile(email)
  def profileListByRole(id: UUID, role: String) = userDao.profileListByRole(id, role)
  def profileByRole(id: UUID, role: String) = userDao.profileByRole(id, role)
  def setRulesAccepted(id: UUID, setting: Boolean) = userDao.setRulesAccepted(id, setting)
  def getRulesAccepted(id: UUID) = userDao.getRulesAccepted(id)

  def assign(crewUUID: UUID, user: User) = user.profiles.headOption match {
    case Some(profile) => userDao.setCrew(crewUUID, profile).map(result => {
      if(result.isLeft && result.left.get > 0) {
        //nats.publishUpdate("USER", user.id)
        userDao.find(user.id).map(_.map(updated => poolService.update(updated))) // Todo: Consider result?!
      }
      result
    })
    case _ => Future.successful(Right("service.user.error.notFound.profile"))
  }

  def assignCrewRole(crew: Crew, role: VolunteerManager, user: User): Future[Either[Int, String]] = {
    crewDao.findDB(crew).flatMap(_ match {
      case Some(crewDB) => user.profiles.headOption match {
        case Some(profile) => userDao.setCrewRole(crew.id, crewDB.id, role, profile).map(result => {
          if(result.isLeft && result.left.get > 0) {
            nats.publishUpdate("USER", user.id)
            userDao.find(user.id).map(_.map(updated => poolService.update(updated))) // Todo: Consider result?!
          }
          result
        })
        case None => Future.successful(Right("service.user.error.notFound.profile"))
      }
      case None => Future.successful(Right("service.user.error.notFound.crew"))
    })
  }

  /**
    * Removes a crew from a user.
    * @author Johann Sell
    * @param user
    */
  def deAssign(user: User) = user.profiles.headOption match {
    case Some(profile) => profile.supporter.crew match {
      case Some(crew) => userDao.removeCrew(crew.id, profile).map(result => {
        if(result.isLeft && result.left.get > 0) {
          nats.publishUpdate("USER", user.id)
          userDao.find(user.id).map(_.map(updated => poolService.update(updated))) // Todo: Consider result?!
        }
        result
      })
      case None => Future.successful(Right("service.user.error.notFound.crew"))
    }
    case None => Future.successful(Right("service.user.error.notFound.profile"))
  }

  def assignOnlyOne(crewUUID: UUID, user: User) = user.profiles.headOption match {
    case Some(profile) if profile.supporter.crew.isDefined => this.deAssign(user).flatMap(_ match {
      case Left(i) if i > 0 => userDao.find(user.id).flatMap(_ match {
        case Some(updatedUser) => this.assign(crewUUID, updatedUser).map(result => {
          if(result.isLeft && result.left.get > 0) {
            nats.publishUpdate("USER", user.id)
            userDao.find(user.id).map(_.map(updated => poolService.update(updated))) // Todo: Consider result?!
          }
          result
        })
        case None => Future.successful(Right("service.user.error.cannotFindUpdatedUser"))
      })
      case Left(i) => Future.successful(Right("service.user.error.oldCrewNotDeleted"))
      case Right(x) => Future.successful(Right(x))
    })
    case Some(profile) => this.assign(crewUUID, user)
    case None => Future.successful(Right("service.user.error.notFound.profile"))
  }
}
