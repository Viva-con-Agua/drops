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
import daos.{AccessRightDao, CrewDao, TaskDao, UserDao, ProfileDao}
import models._
import persistence.pool1.PoolService
import play.api.Logger
import utils.NatsController
import play.api.libs.json._

class requestActiveFlagUserService @Inject() (
                              userDao:UserDao,
                              profileDao:ProfileDao,
                              poolService: PoolService,
                              pool1DBService: Pool1Service,
                              taskDao: TaskDao,
                              crewDao: CrewDao,
                              accessRightDao: AccessRightDao,
                              nats: NatsController
                            ) extends IdentityService[User] {
  val logger: Logger = Logger(this.getClass())

  def retrieve(loginInfo:LoginInfo):Future[Option[User]] = userDao.find(loginInfo)
  def save(user:User) = {
    userDao.save(user).map(user => {
      nats.publishCreate("USER", user.id)
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
    profileDao.updateSupporter(profile).map(profileOpt => {
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
  
  def updateProfileEmail(email: String, profile: Profile) = profileDao.updateProfileEmail(email, profile)
  def getProfile(email: String) = profileDao.getProfile(email)
  def profileListByRole(id: UUID, role: String) = profileDao.profileListByRole(id, role)
  def profileByRole(id: UUID, role: String) = profileDao.profileByRole(id, role)

  def assign(crewUUID: UUID, user: User) = user.profiles.headOption match {
    case Some(profile) => profileDao.setCrew(crewUUID, profile).map(result => {
      if(result.isLeft && result.left.get > 0) {
        nats.publishUpdate("USER", user.id)
        userDao.find(user.id).map(_.map(updated => poolService.update(updated))) // Todo: Consider result?!
      }
      result
    })
    case _ => Future.successful(Right("service.user.error.notFound.profile"))
  }

  def assignCrewRole(crew: Crew, role: VolunteerManager, user: User): Future[Either[Int, String]] = {
    crewDao.findDB(crew).flatMap(_ match {
      case Some(crewDB) => user.profiles.headOption match {
        case Some(profile) => profileDao.setCrewRole(crew.id, crewDB.id, role, profile).map(result => {
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
      case Some(crew) => profileDao.removeCrew(crew.id, profile).map(result => {
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

  /**
    * Calls the profileDao th set the active state to requested on the given profile
    * @param profile
    * @return
    */
  def activeActiveFlag(profile: Profile) = {
    profileDao.setActiveFlag(profile, Some("active")).map(_ match {
      case true => Some("active")
      case false => None
    })
  }

  /**
    * Calls the profileDao to set the active state to requested on the given profile
    * @param profile
    * @return
    */
  def requestActiveFlag(profile: Profile) = {
    profileDao.getActiveFlag(profile).map(_ match {
      case Some(activeFlag) => activeFlag match {
        case "active" => activeFlag
        case "requested" => activeFlag
        case "inactive" => profileDao.setActiveFlag(profile, Some("requested")).map(_ match {
          case true => Some("requested")
          case false => None
        })
      }
      case _ => profileDao.setActiveFlag(profile, Some("requested")).map(_ match {
        case true => Some("requested")
        case false => None
      })
    })
  }

  /**
    * Calls the profileDao to set the active state to inactive on the given profile
    * @param profile
    * @return
    */
  def inactiveActiveFlag(profile: Profile) = {
    profileDao.setActiveFlag(profile, Some("inactive")).map(_ match {
      case true => Some("inactive")
      case false => None
    })
  }

  /**
    * Get the profile with the given id from the userDao
    * @param id
    * @return
    */
  def getProfile(id: UUID) : Future[Option[Profile]] = {
    userDao.find(id).map(_.flatMap(_.profiles.headOption))
  }

  /**
    * Calls the profileDao to get the active state of the given profile
    * @param profile
    * @return
    */
  def checkActiveFlag(profile: Profile) : Future[StatusWithConditions] = {
    // Check for profile existance
    profileDao.getProfile(profile.email.head).map({
      case Some(p) => {
        // Get primary crew of the supporter
        val primaryCrew = hasPrimaryCrew(p)
        // Return the current state
        val status = p.supporter.active match {
          case Some(status) => status
          case _ => "inactive"
        }
        StatusWithConditions(status, Conditions(None, Some(primaryCrew), None))
      }
      case _ => StatusWithConditions("inactive", Conditions(None, Some(false), None))
    })
  }

  /**
    * Calls the profileDao to set the nvm state to the current time + 1 year on the given profile
    * @param profile
    * @return
    */
  def activeNVM(profile: Profile) = {
    profileDao.setNVM(profile, Some(System.currentTimeMillis() + 1000*60*60*365)).map(_ match {
      case true => Some("active")
      case false => None
    })
  }

  /**
    * * Calls the profileDao to set the nvm state to inactive on the given profile
    * @param profile
    * @return
    */
  def inActiveNVM(profile: Profile)  = {
    profileDao.setNVM(profile, None).map(_ match {
      case true => Some("inactive")
      case false => None
    })
  }

  /**
    * Calls the profileDao to get the nvm date of the given profile
    * Checks the preconditions of the nvm state
    * Convert conditions and state it to the nvm states "active", "inactive", "denied", "expired"
    * @param profile
    * @return
    */
  def checkNVM(profile: Profile) : Future[StatusWithConditions] = {
    // Check for profile existance
    profileDao.getProfile(profile.email.head).map(_ match {
      case Some(p) => {
        // Checks if the supporter has an address, a crew and the current nvm date is in the future
        val address = hasAddress(p)
        val primaryCrew = hasPrimaryCrew(p)
        val active = isActive(p)
        val status = p.supporter.nvmDate match {
          case Some(nvmDate) => nvmDate < System.currentTimeMillis() match {
            case true => "expired"
            case false => "active"
          }
          case _ => address && primaryCrew && active match {
            case true => "inactive"
            case false => "denied"
          }
        }
        StatusWithConditions(status, Conditions(Some(address), Some(primaryCrew), Some(active)))
      }
      case _ => StatusWithConditions("denied", Conditions(Some(false), Some(false), Some(false)))
    })
  }

  /**
    * Check if the supporter has an crew
    * @param profile
    * @return Boolean
    */
  private def hasPrimaryCrew(profile: Profile) = {
    profile.supporter.crew match {
      case Some(crew) => true
      case _ => false
    }
  }

  /**
    * Check if the supporter has an address
    * @param profile
    * @return Boolean
    */
  private def hasAddress(profile: Profile) = {
    profile.supporter.address.headOption match {
      case Some(address) => true
      case _ => false
    }
  }

  /**
    * Check if the active state of a supporter in his crew is active
    * @param profile
    * @return
    */
  private def isActive(profile: Profile) = {
    profile.supporter.active match {
      case Some(active) => if(active == "active") { true } else { false }
      case _ => false
    }
  }

}
