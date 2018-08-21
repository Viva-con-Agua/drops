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
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
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
import org.joda.time.DateTime
import persistence.pool1.PoolService
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import java.util.Base64

import play.api.libs.json.{JsError, JsObject, JsValue, Json}
object AuthForms {

  // Sign up
  case class SignUpData(email:String, password:String, firstName:String, lastName:String, mobilePhone:String, placeOfResidence: String, birthday:Date, sex:String)
//  def signUpForm(implicit messages:Messages) = Form(mapping(
//      "email" -> email,
//      "password" -> tuple(
//        "password1" -> nonEmptyText.verifying(minLength(6)),
//        "password2" -> nonEmptyText
//      ).verifying(Messages("error.passwordsDontMatch"), password => password._1 == password._2),
//      "firstName" -> nonEmptyText,
//      "lastName" -> nonEmptyText,
//      "mobilePhone" -> nonEmptyText(minLength = 5).verifying(UserConstraints.telephoneCheck),
//      "placeOfResidence" -> nonEmptyText,
//      "birthday" -> date,
//      "sex" -> nonEmptyText.verifying(UserConstraints.sexCheck)
//    )
//    (
//      (email, password, firstName, lastName, mobilePhone, placeOfResidence, birthday, sex) =>
//        SignUpData(email, password._1, firstName, lastName, mobilePhone, placeOfResidence, birthday, sex)
//    )
//    (signUpData =>
//      Some((signUpData.email, ("",""), signUpData.firstName, signUpData.lastName, signUpData.mobilePhone, signUpData.placeOfResidence, signUpData.birthday, signUpData.sex)))
//  )

  // Sign in
  case class SignInData(email:String, password:String, rememberMe:Boolean)
  object SignInData {
    implicit val signInDataJsonFormat = Json.format[SignInData]
  }
//  val signInForm = Form(mapping(
//      "email" -> email,
//      "password" -> nonEmptyText,
//      "rememberMe" -> boolean
//    )(SignInData.apply)(SignInData.unapply)
//  )

  // Start password recovery
  case class Email(address: String)
  object Email {
    implicit val emailJsonFormat = Json.format[Email]
  }
//  val emailForm = Form(single("email" -> email))

  // Passord recovery
  case class Password(password1: String, password2: String) {
    def valid : Boolean = password1 == password2
  }
  object Password {
    implicit val passwordJsonFormat = Json.format[Password]
  }
//  def resetPasswordForm(implicit messages:Messages) = Form(tuple(
//    "password1" -> nonEmptyText.verifying(minLength(6)),
//    "password2" -> nonEmptyText
//  ).verifying(Messages("error.passwordsDontMatch"), password => password._1 == password._2))
}


class Auth @Inject() (
  val messagesApi: MessagesApi, 
  val env:Environment[User,CookieAuthenticator],
  socialProviderRegistry: SocialProviderRegistry,
  authInfoRepository: AuthInfoRepository,
  credentialsProvider: CredentialsProvider,
  userService: UserService,
  pool1Service: Pool1Service,
  userTokenService: UserTokenService,
  avatarService: AvatarService,
  passwordHasher: PasswordHasher,
  configuration: Configuration,
  pool: PoolService,
  mailer: Mailer,
  dispenserService: DispenserService,
  nats: Nats) extends Silhouette[User,CookieAuthenticator] {

  import AuthForms._

  override val logger: Logger = Logger(this.getClass())

//  def startSignUp = UserAwareAction.async { implicit request =>
//    Future.successful(request.identity match {
//      case Some(user) => redirectAfterLogin
//      case None => {
//        Ok(dispenserService.getTemplate(views.html.auth.startSignUp(signUpForm)))
//    }})
//  }
//
//  def handleStartSignUp = Action.async { implicit request =>
//    signUpForm.bindFromRequest.fold(
//      bogusForm => Future.successful(BadRequest(dispenserService.getTemplate(views.html.auth.startSignUp(bogusForm)))),
//      signUpData => {
//        val loginInfo = LoginInfo(CredentialsProvider.ID, signUpData.email)
//        userService.retrieve(loginInfo).flatMap {
//          case Some(_) =>
//            Future.successful(Redirect(routes.Auth.startSignUp()).flashing(
//              "error" -> Messages("error.userExists", signUpData.email)))
//          case None =>
//            val profile = Profile(loginInfo, signUpData.email, signUpData.firstName, signUpData.lastName, signUpData.mobilePhone, signUpData.placeOfResidence, signUpData.birthday, signUpData.sex, List(new DefaultProfileImage))
//            for {
//              avatarUrl <- avatarService.retrieveURL(signUpData.email)
//              user <- userService.save(User(id = UUID.randomUUID(), profiles =
//                avatarUrl match {
//                  case Some(url) => List(profile.copy(avatar = List(GravatarProfileImage(url),new DefaultProfileImage)))
//                  case _ => List(profile.copy(avatar = List(new DefaultProfileImage)))
//                }))
//              _ <- authInfoRepository.add(loginInfo, passwordHasher.hash(signUpData.password))
//              token <- userTokenService.save(UserToken.create(user.id, signUpData.email, true))
//            } yield {
//              mailer.welcome(profile, link = routes.Auth.signUp(token.id.toString).absoluteURL())
//              Ok(dispenserService.getTemplate(views.html.auth.finishSignUp(profile)))
//            }
//        }
//      }
//    )
//  }
//
//  def signUp(tokenId:String) = Action.async { implicit request =>
//    val id = UUID.fromString(tokenId)
//    userTokenService.find(id).flatMap {
//      case None =>
//        Future.successful(NotFound(views.html.errors.notFound(request)))
//      case Some(token) if token.isSignUp && !token.isExpired =>
//        userService.find(token.userId).flatMap {
//          case None => Future.failed(new IdentityNotFoundException(Messages("error.noUser")))
//          case Some(user) =>
//            val loginInfo = LoginInfo(CredentialsProvider.ID, token.email)
//            for {
//              authenticator <- env.authenticatorService.create(loginInfo)
//              value <- env.authenticatorService.init(authenticator)
//              _ <- userService.confirm(loginInfo)
//              _ <- userTokenService.remove(id)
//              _ <- pool.save(user)    // SEND USER TO POOL 1!
//              result <- env.authenticatorService.embed(value, redirectAfterLogin)
//            } yield result
//        }
//      case Some(token) =>
//        // maybe different error message should be implemented for the case that the given token is expired or not a
//        // signUp token.
//        userTokenService.remove(id).map {_ => NotFound(views.html.errors.notFound(request))}
//    }
//  }
  
  /** Pool1 function
  def pool1SignIn = Action.async { implicit request =>
  }*/


  def identity = UserAwareAction.async { implicit request =>
    Future.successful(request.identity match {
      case Some(user) => Ok(Json.obj("uuid" -> user.id)).as("application/json")
      case _ => generateJsonStatusMsg(request, play.api.mvc.Results.Unauthorized, "error.noAuthenticatedUser", Nil, "AuthProvider", Map[String, String]())

    })
  }

  def authenticate = Action.async(parse.json) { implicit request =>
    val data = request.body.validate[SignInData]
    data.fold(
      errors => Future.successful(generateBogusJson(request, "error.bogusAuthenticationData", Nil, "AuthProvider.Authenticate", JsError.toJson(errors))),
      signInData => {
        val credentials = Credentials(signInData.email, signInData.password)
        pool1Service.pool1user(signInData.email).flatMap {
          case Some(pooluser) if !pooluser.confirmed =>
            userService.retrieve(LoginInfo(CredentialsProvider.ID, signInData.email)).flatMap {
              case None => Future.successful(
                generateJsonStatusMsg(request, play.api.mvc.Results.InternalServerError, "error.missingUserForPool1User", List(signInData.email), "AuthProvider.NoUserForPool1User", Map[String, String]())
              )
              case Some(user) => for {
                token <- userTokenService.save(UserToken.create(user.id, signInData.email, isSignUp = false))
              } yield {
                // Todo: the link should target a drops endpoint that is able to interpretate the token and calls an Arise page afterwards
                mailer.resetPasswordPool1(signInData.email, link = controllers.webapp.routes.Auth.resetPassword(token.id.toString).absoluteURL())
                generateOkJson(request, "status.pool1PasswordResetMailSend", List(signInData.email), "AuthProvider.Pool1PasswordResetMailSend", Map(
                  "user" -> user.id.toString,
                  "email" -> signInData.email
                ))
              }
            }
          case (_) => {
            credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
              userService.retrieve(loginInfo).flatMap {
                case None =>
                  Future.successful(
                    generateJsonStatusMsg(request, play.api.mvc.Results.Unauthorized, "error.noUser", Nil, "AuthProvider.AuthenticateUserNotFound", Map(
                      "email" -> signInData.email
                    ))
                  )
                case Some(user) if !user.profileFor(loginInfo).map(_.confirmed).getOrElse(false) => //"error.unregistered", signInData.email
                  Future.successful(
                    generateJsonStatusMsg(request, play.api.mvc.Results.Unauthorized, "error.unregistered", Nil, "AuthProvider.AuthenticateUserNotConfirmed", Map(
                      "user" -> user.id.toString,
                      "email" -> signInData.email
                    ))
                  )
                case Some(user) => for {
                  authenticator <- env.authenticatorService.create(loginInfo).map {
                    case authenticator if signInData.rememberMe =>
                      val c = configuration.underlying
                      authenticator.copy(
                        expirationDateTime = new DateTime() + c.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry"),
                        idleTimeout = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout"),
                        cookieMaxAge = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.cookieMaxAge")
                      )
                    case authenticator => authenticator
                  }
                  value <- env.authenticatorService.init(authenticator)
                  result <- env.authenticatorService.embed(value, Ok(Json.obj("uuid" -> user.id)))
                } yield result
              }.recover {
                case e:ProviderException =>
                  generateJsonStatusMsg(request, play.api.mvc.Results.Unauthorized, "error.invalidCredentials", Nil, "AuthProvider.ProviderException", Map(
                    "email" -> signInData.email,
                    "providerKey" -> loginInfo.providerKey,
                    "providerID" -> loginInfo.providerID
                  ))
                }
            }
          }
        }
      }
    )
  }

  def signOut = SecuredAction.async { implicit request =>
    userService.retrieve(request.authenticator.loginInfo).flatMap {
      case None =>
        Future.successful(
          generateJsonStatusMsg(request, play.api.mvc.Results.Unauthorized, "error.noUser", Nil, "AuthProvider.AuthenticateUserNotFound", Map(
            "providerKey" -> request.authenticator.loginInfo.providerKey,
            "providerID" -> request.authenticator.loginInfo.providerID
          ))
        )
      case Some(user) => {
        pool.logout(user)
        Future.successful(nats.publishLogout(user.id))
      }
    }
    env.authenticatorService.discard(request.authenticator, generateOkJson(request, "signout.msg", Nil, "AuthProvider.UserLoggedOut"))
  }

//  def startResetPassword = Action { implicit request =>
//    Ok(dispenserService.getTemplate(views.html.auth.startResetPassword(emailForm)))
//  }
//
//  def handlePool1StartResetPassword(email64: String) = Action.async { implicit request =>
//    val email = new String(java.util.Base64.getDecoder.decode(email64), "UTF-8")
//    userService.retrieve(LoginInfo(CredentialsProvider.ID, email)).flatMap {
//        case None => Future.successful(Redirect(routes.Auth.startResetPassword()).flashing("error" -> Messages("error.noUser")))
//        case Some(user) => for {
//          token <- userTokenService.save(UserToken.create(user.id, email, isSignUp = false))
//        } yield {
//          mailer.resetPasswordPool1(email, link = routes.Auth.resetPassword(token.id.toString).absoluteURL())
//          Ok(views.html.auth.resetPasswordInstructionsPool1(email))
//        }
//      }
//  }
//
  def handleResetPasswordStartData = Action.async(parse.json) { implicit request =>
      val data = request.body.validate[Email]
      data.fold(
        errors => Future.successful(generateBogusJson(request, "error.bogusResetPasswordData", Nil, "AuthProvider.HandleStartResetPassword", JsError.toJson(errors))),
        email => userService.retrieve(LoginInfo(CredentialsProvider.ID, email.address)).flatMap {
          case None => Future.successful(
            generateJsonStatusMsg(request, play.api.mvc.Results.NotFound, "error.noUser", Nil, "AuthProvider.ResetPassword", Map(
              "email" -> email.address
            ))
          )
          case Some(user) => for {
            token <- userTokenService.save(UserToken.create(user.id, email.address, isSignUp = false))
          } yield {
            mailer.resetPassword(email.address, link = routes.Auth.resetPassword(token.id.toString).absoluteURL())
            generateOkJson(request, "reset.instructions", List(email.address), "AuthProvider.ResetPassword")
          }
        }
      )
  }

  def resetPassword(tokenId:String) = Action.async { implicit request =>
    val id = UUID.fromString(tokenId)
    userTokenService.find(id).flatMap {
      case None =>
        Future.successful(Redirect("https://localhost/arise#notFound"))
      case Some(token) if !token.isSignUp && !token.isExpired =>
        Future.successful(Redirect("https://localhost/arise#resetPassword?token=" + tokenId))
      case _ => for {
        _ <- userTokenService.remove(id)
      } yield Redirect("https://localhost/arise#notFound")
    }
  }

  def handleResetPassword(tokenId:String) = Action.async(parse.json) { implicit request =>
    val data = request.body.validate[Password]
    data.fold(
      errors => Future.successful(generateBogusJson(request, "error.bogusResetPasswordData", Nil, "AuthProvider.HandleResetPassword", JsError.toJson(errors))),
      passwords => {
        val id = UUID.fromString(tokenId)
        userTokenService.find(id).flatMap {
          case None =>
            Future.successful(
              generateJsonStatusMsg(request, play.api.mvc.Results.NotFound, "error.reset.toToken", List(tokenId), "AuthProvider.ResetPassword", Map(
                "tokenId" -> tokenId
              ))
            )
          case Some(token) if !passwords.valid =>
            Future.successful(
              generateBogusJson(request, "error.bogusResetPasswordData", Nil, "AuthProvider.HandleResetPassword", Json.toJson(Map("error" -> "not valid")))
            )
          case Some(token) if !token.isSignUp && !token.isExpired =>
            val loginInfo = LoginInfo(CredentialsProvider.ID, token.email)
            for {
              _ <- authInfoRepository.save(loginInfo, passwordHasher.hash(passwords.password1))
              authenticator <- env.authenticatorService.create(loginInfo)
              value <- env.authenticatorService.init(authenticator)
              _ <- userTokenService.remove(id)
              //pool1 user
              _ <- pool1Service.confirmed(token.email)
              _ <- userService.confirm(loginInfo)
              result <- env.authenticatorService.embed(value, generateOkJson(request, "reset.done", Nil, "AuthProvider.ResetPassword"))
            } yield result
        }
      }
    )
  }

  protected def generateBogusJson(request: RequestHeader, msg: String, msgValues: List[String], internalErrorCode: String, errors: JsValue): Result = {
    generateJsonStatusMsg(request, play.api.mvc.Results.BadRequest, msg, msgValues, internalErrorCode, errors)
  }

  protected def generateOkJson(request: RequestHeader, msg: String, msgValues: List[String], internalStatusCode: String, additional : Map[String, String] = Map()): Result = {
    generateJsonStatusMsg(request, play.api.mvc.Results.Ok, msg, msgValues, internalStatusCode, additional)
  }

  protected def generateJsonStatusMsg(request: RequestHeader, code: play.api.mvc.Results.Status, msg: String, msgValues: List[String], internalErrorCode: String, additional: Map[String, String] = Map()) : Result =
    generateJsonStatusMsg(request, code, msg, msgValues, internalErrorCode, Json.toJson(additional))

  protected def generateJsonStatusMsg(request: RequestHeader, code: play.api.mvc.Results.Status, msg: String, msgValues: List[String], internalErrorCode: String, additional: JsValue) : Result =
    request.accepts("application/json") match {
      case true => code(Json.obj(
        "internal_error_code" -> (code.header.status + "." + internalErrorCode),
        "http_error_code" -> code.header.status,
        "msg_i18n" -> msg,
        "msg" -> Messages(msg, msgValues:_*),
        "additional_information" -> additional
      )).as("application/json")
      case _ => generateStatusMsg(request, code, msg, additional)
    }

  protected def generateStatusMsg(request: RequestHeader, code: play.api.mvc.Results.Status, msg: String, additional: JsValue) : Result =
    code(Messages(msg))
//
//  def socialAuthenticate(providerId:String) = UserAwareAction.async { implicit request =>
//    (socialProviderRegistry.get[SocialProvider](providerId) match {
//      case Some(p:SocialProvider with CommonSocialProfileBuilder) => p.authenticate.flatMap {
//        case Left(result) => Future.successful(result)
//        case Right(authInfo) => for {
//          profile <- p.retrieveProfile(authInfo)
//          user <- request.identity.fold(userService.save(profile))(userService.link(_,profile))
//          authInfo <- authInfoRepository.save(profile.loginInfo, authInfo)
//          authenticator <- env.authenticatorService.create(profile.loginInfo)
//          value <- env.authenticatorService.init(authenticator)
//          result <- env.authenticatorService.embed(value, redirectAfterLogin)
//        } yield result
//      }
//      case _ => Future.successful(
//        Redirect(request.identity.fold(routes.Auth.signIn())(_ => routes.Application.profile())).flashing(
//          "error" -> Messages("error.noProvider", providerId))
//      )
//    }).recover {
//      case e:ProviderException =>
//        logger.error("Provider error", e)
//        Redirect(request.identity.fold(routes.Auth.signIn())(_ => routes.Application.profile()))
//          .flashing("error" -> Messages("error.notAuthenticated", providerId))
//    }
//  }
//
//  /**
//    * Defines the URL that is used after login.
//    *
//    * @return
//    */
//  def redirectAfterLogin : Result = {
//    val default = Redirect(routes.Application.index())
//
//    configuration.getBoolean("login.flow.ms.switch").map(_ match {
//      case true => configuration.getString("login.flow.ms.url").map(
//        (url) => Redirect( url )
//      ).getOrElse(
//        default
//      )
//      case false => default
//    }).getOrElse(
//      default
//    )
//  }
//
//  /**
//    * Defines the URL that is used after logout.
//    *
//    * @return
//    */
//  def redirectAfterLogout : Result = {
//    Redirect(routes.Auth.signIn)
//  }
}
