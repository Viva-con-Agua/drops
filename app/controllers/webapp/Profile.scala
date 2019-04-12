package controllers.webapp

import java.text.SimpleDateFormat
import java.util.{Date, UUID}

import javax.inject.Inject
import java.util.Properties

import org.nats._

import scala.concurrent.Future
import scala.concurrent.duration._
import net.ceedubs.ficus.Ficus._
import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api.{Environment, LoginInfo, Silhouette}
import com.mohiva.play.silhouette.api.util.Base64
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.{Credentials, PasswordHasher}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.exceptions.{IdentityNotFoundException, InvalidPasswordException}
import com.mohiva.play.silhouette.impl.providers._
import play.api._
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.mvc._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits._
import models._
import models.dispenser._
import services.{ Pool1Service, UserService, UserTokenService}
import utils.{Mailer, Nats}
import daos.{CrewDao, OauthClientDao, TaskDao}
import org.joda.time.DateTime
import persistence.pool1.PoolService
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import java.util.Base64
import play.api.libs.ws._
import play.api.libs.json.{JsError, JsObject, JsValue, Json, Reads}

// UserData for Profile Page
case class ProfileData(email: Option[String], firstName : Option[String], lastName: Option[String], mobilePhone: Option[String], placeOfResidence: Option[String], birthday:Option[Long], sex:Option[String], address:Option[Set[Address]], active: Option[String], nvmDate:Option[Long])
object ProfileData {
  implicit val userDataJsonFormat = Json.format[ProfileData]    
}


class Profile @Inject() (
  oauth2ClientDao: OauthClientDao,
  userService: UserService,
  crewDao: CrewDao,
  taskDao: TaskDao,
  ws: WSClient,
  passwordHasher: PasswordHasher,
  configuration: Configuration,
  socialProviderRegistry: SocialProviderRegistry,
  val messagesApi: MessagesApi,
  val env: Environment[User, CookieAuthenticator]
) extends Silhouette[User, CookieAuthenticator]  {
 implicit val mApi = messagesApi
  override val logger = Logger(Action.getClass)


  /*def get = SecuredAction.async { implicit request =>
    WebAppResult.Ok(userService.getProfile(request.authenticator.loginInfo.email))
  }*/
  
  def validateJson[A: Reads] = BodyParsers.parse.json.validate(_.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e))))

  def getUser(uuid: String) = UserAwareAction.async { implicit request =>
    request.identity match {
      case Some( u ) => userService.find(UUID.fromString(uuid)).map(_ match {
        case Some(user) => WebAppResult.Ok(request, "profile.found.user", Nil, "Profile.Found.User", Json.toJson(user)).getResult
        case None => WebAppResult.NotFound(request, "profile.notFound.user", Nil, "Profile.NotFound.User", Map[String, String]("uuid" -> uuid) ).getResult
      })
      case None => Future.successful(
        WebAppResult.Unauthorized(request, "error.noAuthenticatedUser", Nil, "AuthProvider.Identity.Unauthorized", Map[String, String]()).getResult
      )
    }
  }

  def get = UserAwareAction.async { implicit request =>
    request.identity match {
        case Some(user) => {
        userService.find(user.id).flatMap {
          case Some(u) => {
            var profiles = List[ProfileData]()
            u.profiles.foreach( profile => {
              val entry = ProfileData(
                profile.email, 
                profile.supporter.firstName,
                profile.supporter.lastName,
                profile.supporter.mobilePhone,
                profile.supporter.placeOfResidence,
                profile.supporter.birthday,
                profile.supporter.sex,
                Some(profile.supporter.address),
                profile.supporter.active,
                profile.supporter.nvmDate
              ) 
              profiles = entry :: profiles
            })
            Future.successful(WebAppResult.Ok(request, "profile.get", Nil, "Profile.get.successful", Json.toJson(profiles)).getResult)
          }
          case _ => Future.successful(WebAppResult.Bogus(request, "profile.get", Nil, "Profile.get.profileNotFound", Json.obj("user" -> "error")).getResult)
        }
      }
      case _ => Future.successful(WebAppResult.Unauthorized(request, "error.noAuthenticatedUser", Nil, "AuthProvider.Identity.Unauthorized", Map[String, String]()).getResult)
    }
  }
  
  def update = UserAwareAction.async(validateJson[ProfileData]) { implicit request =>
    request.identity match {
      case Some(currentUser) =>{
        request.body.email match {
          case Some(email) => {
            userService.getProfile(email).flatMap {
              case Some(profile) => {
                val fullName = request.body.firstName match {
                  case Some(firstName) => {
                    request.body.lastName match {
                      case Some(lastName) => s"${firstName} ${lastName}"
                      case None => firstName
                    }
                  }
                  case None => {
                    request.body.lastName match {
                      case Some(lastName) => lastName
                      case None => ""
                    }
                  }
                } 
                val supporter = Supporter(
                  request.body.firstName,
                  request.body.lastName,
                  Option(fullName),
                  request.body.mobilePhone,
                  request.body.placeOfResidence,
                  request.body.birthday,
                  request.body.sex,
                  profile.supporter.crew,
                  profile.supporter.roles,
                  profile.supporter.pillars,
                  request.body.address match {
                    case Some(address) => address
                    case _ => profile.supporter.address
                  },
                  profile.supporter.active,
                  profile.supporter.nvmDate
                )
                val newProfile = profile.copy(supporter = supporter)
                //val newProfile = Profile(profile.loginInfo, profile.confirmed, profile.email, supporter, profile.passwordInfo, profile.oauth1Info, profile.avatar)
                userService.updateSupporter(currentUser.id, newProfile).map({
                  case Some(profile) => WebAppResult.Ok(request, "profile.update", Nil, "AuthProvider.Identity.Success", Json.toJson(profile)).getResult
                  case None => WebAppResult.Bogus(request, "profile.notExist", Nil, "402", Json.toJson(request.body)).getResult
                })
              }
              case _ => Future.successful(WebAppResult.Bogus(request, "profile.NotExist", Nil, "402", Json.toJson(request.body)).getResult)
            }
          }
          case _ => Future.successful(WebAppResult.Bogus(request, "profile.emailNotExist", Nil, "402", Json.toJson(request.body)).getResult)
       }
      }
    case _ => Future.successful(WebAppResult.Unauthorized(request, "error.noAuthenticatedUser", Nil, "AuthProvider.Identity.Unauthorized", Map[String, String]()).getResult)
    }
  }

  def assignCrew(uuidCrew: String) = UserAwareAction.async { implicit request =>
    request.identity match {
      case Some(user) => userService.assignOnlyOne(UUID.fromString(uuidCrew), user).map(_ match {
        case Left(i) if i > 0 => WebAppResult.Ok(request, "profile.assign.crew.success", Nil, "Profile.Assign.Crew.Success", Json.obj()).getResult
        case Left(i) => WebAppResult.Bogus(request, "profile.assign.error.nothingAssigned", Nil, "Profile.Assign.Error.NothingAssigned", Json.obj(
          "crewID" -> uuidCrew,
          "userID" -> user.id
        )).getResult
        case Right(msg) => WebAppResult.NotFound(request, msg, Nil, msg, Map(
          "crewID" -> uuidCrew.toString,
          "userID" -> user.id.toString
        )).getResult
      })
      case _ => Future.successful(WebAppResult.Unauthorized(request, "error.noAuthenticatedUser", Nil, "AuthProvider.Identity.Unauthorized", Map[String, String]()).getResult)
    }
  }

  def removeCrew = UserAwareAction.async { implicit request =>
    request.identity match {
      case Some(user) => userService.deAssign(user).map(_ match {
        case Left(i) if i > 0 => WebAppResult.Ok(request, "profile.removeAssign.crew.success", Nil, "Profile.RemoveAssign.Crew.Success", Json.obj()).getResult
        case Left(i) => WebAppResult.Bogus(request, "profile.removeAssign.error.nothingAssigned", Nil, "Profile.RemoveAssign.Error.NothingAssigned", Json.obj(
          "userID" -> user.id
        )).getResult
        case Right(msg) => WebAppResult.NotFound(request, msg, Nil, msg, Map(
          "userID" -> user.id.toString
        )).getResult
      })
      case _ => Future.successful(WebAppResult.Unauthorized(request, "error.noAuthenticatedUser", Nil, "AuthProvider.Identity.Unauthorized", Map[String, String]()).getResult)
    }
  }

  def assignRole(userUUID: String, pillar: String) = UserAwareAction.async { implicit request =>
    val otherUserID = UUID.fromString(userUUID)
    request.identity match {
      case Some(user) => userService.find(otherUserID).flatMap(_ match {
        case Some(otherUser) => {
          val p = Pillar(pillar)
          p.isKnown match {
            case true => user.profiles.headOption match {
              case Some(profile) => profile.supporter.crew match {
                case Some(crew) => userService.assignCrewRole(crew, VolunteerManager(crew, p), otherUser).map(_ match {
                  case Left(i) => WebAppResult.Ok(request, "profile.assignRole.success", Nil, "Profile.AssignRole.Success", Json.obj()).getResult
                  case Right(error) => WebAppResult.NotFound(request, error, Nil, error, Map[String, String]()).getResult
                })
                case None => Future.successful(WebAppResult.NotFound(request, "profile.assignRole.crewNotFoundExecutingUser", Nil, "Profile.AssignRole.CrewNotFoundExecutingUser", Map[String, String]()).getResult)
              }
              case None => Future.successful(WebAppResult.NotFound(request, "profile.assignRole.profileNotFoundExecutingUser", Nil, "Profile.AssignRole.ProfileNotFoundExecutingUser", Map[String, String]()).getResult)
            }
            case false => Future.successful(WebAppResult.NotFound(request, "profile.assignRole.givenPillarUnknown", Nil, "Profile.AssignRole.GivePillarUnknown", Map[String, String]()).getResult)
          }
        }
        case None => Future.successful(WebAppResult.NotFound(request, "profile.assignRole.otherUserNotFound", Nil, "Profile.AssignRole.OtherUserNotFound", Map[String, String]()).getResult)
      })
      case None => Future.successful(WebAppResult.Unauthorized(request, "error.noAuthenticatedUser", Nil, "AuthProvider.Identity.Unauthorized", Map[String, String]()).getResult)
    }
  }

  def getNewsletterSettingsPool1 = UserAwareAction.async { implicit request =>
    request.identity match {
      case Some(user) => userService.getNewsletterPool1Settings(user.id).map(_ match {
        case Some(setting) => WebAppResult.Ok(request, "profile.getNewsletterSetting.success", Nil, "Profile.GetNewsletterSetting.Success", Json.obj("setting" -> setting)).getResult
        case None => WebAppResult.NotFound(request, "profile.getNewsletterSetting.notFound", Nil, "Profile.GetNewsletterSetting.NotFound", Map[String, String]()).getResult
      })
      case None => Future.successful(WebAppResult.Unauthorized(request, "error.noAuthenticatedUser", Nil, "AuthProvider.Identity.Unauthorized", Map[String, String]()).getResult)
    }
  }

  def setNewsletterSettingsPool1(setting: String) = UserAwareAction.async { implicit request =>
    request.identity match {
      case Some(user) => userService.setNewsletterPool1Settings(user, setting).map(_ match {
        case true => WebAppResult.Ok(request, "profile.setNewsletterSetting.success", Nil, "Profile.SetNewsletterSetting.Success", Json.obj("setting" -> setting)).getResult
        case false => WebAppResult.Bogus(request, "profile.setNewsletterSetting.bogus", Nil, "Profile.SetNewsletterSetting.Bogus", Json.obj("setting" -> setting)).getResult
      })
      case None => Future.successful(WebAppResult.Unauthorized(request, "error.noAuthenticatedUser", Nil, "AuthProvider.Identity.Unauthorized", Map[String, String]()).getResult)
    }
  }
  /**
   * NVM = non voting membership
   * The controller will check if a user is in the possibility to be non voting member.
   */

  def requestNVM = UserAwareAction.async { implicit request =>
    request.identity match {
      //dummy function. Validation test not implemented
      case Some(user) => Future.successful(WebAppResult.Ok(request, "profile.requestNVM.success", Nil, "Profile.requestNVM.success", Json.obj("status" -> "in progress")).getResult)
      case None => Future.successful(WebAppResult.Unauthorized(request, "error.noAuthenticatedUser", Nil, "AuthProvider.Identity.Unauthorized", Map[String, String]()).getResult)
    }
  }

  def checkNVM = UserAwareAction.async { implicit request =>
    request.identity match {
      //dummy function. Need Validation from requestNVM for check if a user is NVM
      //case Some(user) => Future.successful(WebAppResult.Ok(request, "profile.checkNVM.success", Nil, "Profile.checkNVM.success", Json.obj("status" -> "denied", "conditions" -> Json.obj("hasAddress" -> false, "hasPrimaryCrew" -> false, "isActive" -> false))).getResult)
      case Some(user) => {
        user.profiles.headOption match {
          case Some(profile) => userService.checkNVM(profile).map(nvmState =>
            WebAppResult.Ok(request, "profile.checkNVM.success", Nil, "Profile.checkNVM.success", nvmState).getResult
          )
          case None => Future.successful(WebAppResult.NotFound(request, "profile.checkNVM.notFound", Nil, "Profile.checkNVM.notFound", Map[String, String]()).getResult)
        }
      }
      case None => Future.successful(WebAppResult.Unauthorized(request, "error.noAuthenticatedUser", Nil, "AuthProvider.Identity.Unauthorized", Map[String, String]()).getResult)
    }
  }

  /**
    * Active flag
    * The controller will check if a user is an active supporter of his main crew.
    * Store the user in a list for Network-ASP to check if the request is valid
    */
  def checkActiveFlag = UserAwareAction.async { implicit request =>
    request.identity match {
      //dummy function. Need Validation from requestNVM for check if a user is NVM
      //case Some(user) => userService.checkActiveFlag()
      case Some(user) => {
        user.profiles.headOption match {
          case Some(profile) => userService.checkActiveFlag(profile).map(activeState =>
            WebAppResult.Ok(request, "profile.checkActiveFlag.success", Nil, "Profile.checkActiveFlag.success", activeState).getResult
          )
          case None => Future.successful(WebAppResult.NotFound(request, "profile.checkActiveFlag.notFound", Nil, "Profile.checkActiveFlag.notFound", Map[String, String]()).getResult)
        }
      }
      case None => Future.successful(WebAppResult.Unauthorized(request, "error.noAuthenticatedUser", Nil, "AuthProvider.Identity.Unauthorized", Map[String, String]()).getResult)
    }
  }

  def requestActive = UserAwareAction.async { implicit request =>
    request.identity match {
      //dummy function. Validation test not implemented
      case Some(user) => Future.successful(WebAppResult.Ok(request, "profile.requestActiveFlag.success", Nil, "Profile.requestActiveFlag.success", Json.obj("status" -> "requested")).getResult)
      case None => Future.successful(WebAppResult.Unauthorized(request, "error.noAuthenticatedUser", Nil, "AuthProvider.Identity.Unauthorized", Map[String, String]()).getResult)
    }
  }

}
