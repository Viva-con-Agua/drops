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
import services.{DispenserService, Pool1Service, UserService, UserTokenService}
import utils.{Mailer, Nats}
import daos.{CrewDao, OauthClientDao, TaskDao}
import org.joda.time.DateTime
import persistence.pool1.PoolService
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import java.util.Base64
import play.api.libs.ws._
import play.api.libs.json.{JsError, JsObject, JsValue, Json, Reads}

// UserData for Profile Page
case class ProfileData(email: Option[String], firstName : Option[String], lastName: Option[String], mobilePhone: Option[String], placeOfResidence: Option[String], birthday:Option[Long], sex:Option[String])
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
  dispenserService: DispenserService,
  socialProviderRegistry: SocialProviderRegistry,
  val messagesApi: MessagesApi,
  val env: Environment[User, CookieAuthenticator]
) extends Silhouette[User, CookieAuthenticator]  {
 implicit val mApi = messagesApi


  /*def get = SecuredAction.async { implicit request =>
    WebAppResult.Ok(userService.getProfile(request.authenticator.loginInfo.email))
  }*/
  
  def validateJson[A: Reads] = BodyParsers.parse.json.validate(_.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e))))

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
                profile.supporter.sex
              ) 
              profiles = entry :: profiles
            })
            Future.successful(WebAppResult.Ok(request, "profile.get", Nil, "AuthProvider.Identity.Success", Json.toJson(profiles)).getResult)
          }
          case _ => Future.successful(Ok("blabla"))
        }
      }
      case _ => Future.successful(Ok("blabla"))
    }
  }
  
  def update = UserAwareAction.async(validateJson[ProfileData]) { implicit request =>
    request.identity match {
      case Some(currentUser) =>{
        request.body.email match {
          case Some(email) => {
            userService.getProfile(email).flatMap {
              case Some(profile) => {
                val supporter = Supporter(
                  request.body.firstName,
                  request.body.lastName,
                  None,
                  request.body.mobilePhone,
                  request.body.placeOfResidence,
                  request.body.birthday,
                  request.body.sex,
                  profile.supporter.crew,
                  profile.supporter.pillars
                )
                val newProfile = Profile(profile.loginInfo, profile.confirmed, profile.email, supporter, profile.passwordInfo, profile.oauth1Info, profile.avatar)
                userService.updateProfile(currentUser.id, newProfile).map({
                  case Some(profile) => WebAppResult.Ok(request, "profile.update", Nil, "AuthProvider.Identity.Success", Json.toJson(request.body)).getResult
                  case None => Ok("will nicht")
                })
              }
              case _ => Future.successful(Ok("will nicht"))
            }
          }
          case _ => Future.successful(Ok("will nicht"))
       }
      }
    case _ => Future.successful(Ok("will nicht"))
    }
  }
}
