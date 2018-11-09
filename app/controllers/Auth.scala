package controllers

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
import com.mohiva.play.silhouette.api.util.{Base64}
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
import UserForms.UserConstraints
import services.{UserService, UserTokenService, Pool1Service, DispenserService}
import utils.{Mailer, Nats}
import org.joda.time.DateTime
import persistence.pool1.PoolService
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import java.util.Base64
object AuthForms {

  // Sign up
  case class SignUpData(email:String, password:String, firstName:String, lastName:String, mobilePhone:String, placeOfResidence: String, birthday:Date, sex:String)
  def signUpForm(implicit messages:Messages) = Form(mapping(
      "email" -> email,
      "password" -> tuple(
        "password1" -> nonEmptyText.verifying(minLength(6)),
        "password2" -> nonEmptyText
      ).verifying(Messages("error.passwordsDontMatch"), password => password._1 == password._2),
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "mobilePhone" -> nonEmptyText(minLength = 5).verifying(UserConstraints.telephoneCheck),
      "placeOfResidence" -> nonEmptyText,
      "birthday" -> date,
      "sex" -> nonEmptyText.verifying(UserConstraints.sexCheck)
    )
    (
      (email, password, firstName, lastName, mobilePhone, placeOfResidence, birthday, sex) =>
        SignUpData(email, password._1, firstName, lastName, mobilePhone, placeOfResidence, birthday, sex)
    )
    (signUpData =>
      Some((signUpData.email, ("",""), signUpData.firstName, signUpData.lastName, signUpData.mobilePhone, signUpData.placeOfResidence, signUpData.birthday, signUpData.sex)))
  )

  // Sign in
  case class SignInData(email:String, password:String, rememberMe:Boolean)
  val signInForm = Form(mapping(
      "email" -> email,
      "password" -> nonEmptyText,
      "rememberMe" -> boolean
    )(SignInData.apply)(SignInData.unapply)
  )

  // Start password recovery
  val emailForm = Form(single("email" -> email))

  // Passord recovery
  def resetPasswordForm(implicit messages:Messages) = Form(tuple(
    "password1" -> nonEmptyText.verifying(minLength(6)),
    "password2" -> nonEmptyText
  ).verifying(Messages("error.passwordsDontMatch"), password => password._1 == password._2))
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

  def init = Action.async { implicit request => {
    val config = configuration.getConfig("admin.default")
    userService.retrieve(LoginInfo(CredentialsProvider.ID, config.get.getString("email").get)).flatMap(_ match {
      case Some(user) => Future.successful(Redirect("/"))
      case None => {
        val df = new SimpleDateFormat("yyyy-mm-dd")
        val loginInfo = LoginInfo(CredentialsProvider.ID, config.get.getString("email").get)
        val profile = Profile(
          loginInfo,
          config.get.getString("email").get,
          config.get.getString("name.first").get,
          config.get.getString("name.last").get,
          config.get.getString("mobilephone").get,
          config.get.getString("placeOfResidence").get,
          df.parse(config.get.getString("birthday").get),
          config.get.getString("sex").get,
          List(new DefaultProfileImage)
        )
        for {
          user <- userService.save(User(id = UUID.randomUUID(), profiles = List(profile.copy(avatar = List(new DefaultProfileImage))), roles = Set(RoleAdmin), updated = System.currentTimeMillis(), created = System.currentTimeMillis()))
          _ <- authInfoRepository.add(loginInfo, passwordHasher.hash(config.get.getString("password").get))
          token <- userTokenService.save(UserToken.create(user.id, config.get.getString("email").get, true))
        } yield {
          mailer.welcome(profile, link = routes.Auth.signUp(token.id.toString).absoluteURL())
          Ok(dispenserService.getTemplate(views.html.auth.finishSignUp(profile)))
        }
      }
    })
  }}

  def startSignUp = UserAwareAction.async { implicit request =>
    Future.successful(request.identity match {
      case Some(user) => redirectAfterLogin
      case None => {
        Ok(dispenserService.getTemplate(views.html.auth.startSignUp(signUpForm)))
    }})
  }

  def handleStartSignUp = Action.async { implicit request =>
    signUpForm.bindFromRequest.fold(
      bogusForm => Future.successful(BadRequest(dispenserService.getTemplate(views.html.auth.startSignUp(bogusForm)))),
      signUpData => {
        val loginInfo = LoginInfo(CredentialsProvider.ID, signUpData.email)
        userService.retrieve(loginInfo).flatMap {
          case Some(_) => 
            Future.successful(Redirect(routes.Auth.startSignUp()).flashing(
              "error" -> Messages("error.userExists", signUpData.email)))
          case None =>
            val profile = Profile(loginInfo, signUpData.email, signUpData.firstName, signUpData.lastName, signUpData.mobilePhone, signUpData.placeOfResidence, signUpData.birthday, signUpData.sex, List(new DefaultProfileImage))
            for {
              avatarUrl <- avatarService.retrieveURL(signUpData.email)
              user <- userService.save(User(id = UUID.randomUUID(), profiles =
                avatarUrl match {
                  case Some(url) => List(profile.copy(avatar = List(GravatarProfileImage(url),new DefaultProfileImage)))
                  case _ => List(profile.copy(avatar = List(new DefaultProfileImage)))
                }, updated = System.currentTimeMillis(), created = System.currentTimeMillis()))
              _ <- authInfoRepository.add(loginInfo, passwordHasher.hash(signUpData.password))
              token <- userTokenService.save(UserToken.create(user.id, signUpData.email, true))
            } yield {
              mailer.welcome(profile, link = routes.Auth.signUp(token.id.toString).absoluteURL())
              Ok(dispenserService.getTemplate(views.html.auth.finishSignUp(profile)))
            }
        }
      }
    )
  }

  def signUp(tokenId:String) = Action.async { implicit request =>
    val id = UUID.fromString(tokenId)
    userTokenService.find(id).flatMap {
      case None => 
        Future.successful(NotFound(views.html.errors.notFound(request)))
      case Some(token) if token.isSignUp && !token.isExpired => 
        userService.find(token.userId).flatMap {
          case None => Future.failed(new IdentityNotFoundException(Messages("error.noUser")))
          case Some(user) => 
            val loginInfo = LoginInfo(CredentialsProvider.ID, token.email)
            for {
              authenticator <- env.authenticatorService.create(loginInfo)
              value <- env.authenticatorService.init(authenticator)
              _ <- userService.confirm(loginInfo)
              _ <- userTokenService.remove(id)
              _ <- pool.save(user)    // SEND USER TO POOL 1!
              result <- env.authenticatorService.embed(value, redirectAfterLogin)
            } yield result
        }
      case Some(token) =>
        // maybe different error message should be implemented for the case that the given token is expired or not a
        // signUp token.
        userTokenService.remove(id).map {_ => NotFound(views.html.errors.notFound(request))}
    }
  }
  
  /** Pool1 function
  def pool1SignIn = Action.async { implicit request =>
  }*/


  def signIn = UserAwareAction.async { implicit request =>
    Future.successful(request.identity match {
      case Some(user) => Redirect(routes.Application.index())
      case None => {
        Ok(dispenserService.getTemplate(views.html.auth.signIn(signInForm, socialProviderRegistry)))
      }
    })
  }


  def authenticate = Action.async { implicit request =>
    signInForm.bindFromRequest.fold(
      bogusForm => Future.successful(BadRequest(dispenserService.getTemplate(views.html.auth.signIn(bogusForm, socialProviderRegistry)))),
      signInData => {
        val credentials = Credentials(signInData.email, signInData.password)
        pool1Service.pool1user(signInData.email).flatMap {
          case Some(pooluser) if !pooluser.confirmed =>
            Future.successful(Redirect(routes.Auth.handlePool1StartResetPassword(java.util.Base64.getEncoder.encodeToString(signInData.email.getBytes("UTF-8")))).flashing("error" -> Messages("error.pool1userinit")))
          case (_) => {
        credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
          userService.retrieve(loginInfo).flatMap {
            case None => 
              Future.successful(Redirect(routes.Auth.signIn()).flashing("error" -> Messages("error.noUser")))
            case Some(user) if !user.profileFor(loginInfo).map(_.confirmed).getOrElse(false) =>
              Future.successful(Redirect(routes.Auth.signIn()).flashing("error" -> Messages("error.unregistered", signInData.email)))
            case Some(_) => for {
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
              result <- env.authenticatorService.embed(value, redirectAfterLogin)
            } yield result
          }.recover {
            case e:ProviderException =>
              Redirect(routes.Auth.signIn()).flashing("error" -> Messages("error.invalidCredentials"))
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
        Future.successful(Redirect(routes.Auth.signOut()).flashing("error" -> Messages("error.noUser")))
      case Some(user) => {
        pool.logout(user)
        Future.successful(nats.publishLogout(user.id))

      }
    }
    env.authenticatorService.discard(request.authenticator, redirectAfterLogout)
    
  }

  def startResetPassword = Action { implicit request =>
    Ok(dispenserService.getTemplate(views.html.auth.startResetPassword(emailForm)))
  }

  def handlePool1StartResetPassword(email64: String) = Action.async { implicit request =>
    val email = new String(java.util.Base64.getDecoder.decode(email64), "UTF-8")
    userService.retrieve(LoginInfo(CredentialsProvider.ID, email)).flatMap {
        case None => Future.successful(Redirect(routes.Auth.startResetPassword()).flashing("error" -> Messages("error.noUser")))
        case Some(user) => for {
          token <- userTokenService.save(UserToken.create(user.id, email, isSignUp = false))
        } yield {
          mailer.resetPasswordPool1(email, link = routes.Auth.resetPassword(token.id.toString).absoluteURL())
          Ok(views.html.auth.resetPasswordInstructionsPool1(email))
        }
      }
  }

  def handleStartResetPassword = Action.async { implicit request =>
    emailForm.bindFromRequest.fold(
      bogusForm => Future.successful(BadRequest(dispenserService.getTemplate(views.html.auth.startResetPassword(bogusForm)))),
      email => userService.retrieve(LoginInfo(CredentialsProvider.ID, email)).flatMap {
        case None => Future.successful(Redirect(routes.Auth.startResetPassword()).flashing("error" -> Messages("error.noUser")))
        case Some(user) => for {
          token <- userTokenService.save(UserToken.create(user.id, email, isSignUp = false))
        } yield {
          mailer.resetPassword(email, link = routes.Auth.resetPassword(token.id.toString).absoluteURL())
          Ok(dispenserService.getTemplate(views.html.auth.resetPasswordInstructions(email)))
        }
      }
    )
  }

  def resetPassword(tokenId:String) = Action.async { implicit request =>
    val id = UUID.fromString(tokenId)
    userTokenService.find(id).flatMap {
      case None => 
        Future.successful(NotFound(views.html.errors.notFound(request)))
      case Some(token) if !token.isSignUp && !token.isExpired => 
        Future.successful(Ok(dispenserService.getTemplate(views.html.auth.resetPassword(tokenId, resetPasswordForm))))
      case _ => for {
        _ <- userTokenService.remove(id)
      } yield NotFound(views.html.errors.notFound(request))
    }
  }

  def handleResetPassword(tokenId:String) = Action.async { implicit request =>
    resetPasswordForm.bindFromRequest.fold(
      bogusForm => Future.successful(BadRequest(dispenserService.getTemplate(views.html.auth.resetPassword(tokenId, bogusForm)))),
      passwords => {
        val id = UUID.fromString(tokenId)
        userTokenService.find(id).flatMap {
          case None => 
            Future.successful(NotFound(views.html.errors.notFound(request)))
          case Some(token) if !token.isSignUp && !token.isExpired =>
            val loginInfo = LoginInfo(CredentialsProvider.ID, token.email)
            for {
              _ <- authInfoRepository.save(loginInfo, passwordHasher.hash(passwords._1))
              authenticator <- env.authenticatorService.create(loginInfo)
              value <- env.authenticatorService.init(authenticator)
              _ <- userTokenService.remove(id)
              //pool1 user
              _ <- pool1Service.confirmed(token.email)
              _ <- userService.confirm(loginInfo)
              result <- env.authenticatorService.embed(value, Ok(dispenserService.getTemplate(views.html.auth.resetPasswordDone())))
            } yield result
        }
      } 
    )
  }

  def socialAuthenticate(providerId:String) = UserAwareAction.async { implicit request =>
    (socialProviderRegistry.get[SocialProvider](providerId) match {
      case Some(p:SocialProvider with CommonSocialProfileBuilder) => p.authenticate.flatMap {
        case Left(result) => Future.successful(result)
        case Right(authInfo) => for {
          profile <- p.retrieveProfile(authInfo)
          user <- request.identity.fold(userService.save(profile))(userService.link(_,profile))
          authInfo <- authInfoRepository.save(profile.loginInfo, authInfo)
          authenticator <- env.authenticatorService.create(profile.loginInfo)
          value <- env.authenticatorService.init(authenticator)
          result <- env.authenticatorService.embed(value, redirectAfterLogin)
        } yield result
      } 
      case _ => Future.successful(
        Redirect(request.identity.fold(routes.Auth.signIn())(_ => routes.Application.profile())).flashing(
          "error" -> Messages("error.noProvider", providerId))
      )
    }).recover {
      case e:ProviderException => 
        logger.error("Provider error", e)
        Redirect(request.identity.fold(routes.Auth.signIn())(_ => routes.Application.profile()))
          .flashing("error" -> Messages("error.notAuthenticated", providerId))
    }
  }

  /**
    * Defines the URL that is used after login.
    *
    * @return
    */
  def redirectAfterLogin : Result = {
    val default = Redirect(routes.Application.index())

    configuration.getBoolean("login.flow.ms.switch").map(_ match {
      case true => configuration.getString("login.flow.ms.url").map(
        (url) => Redirect( url )
      ).getOrElse(
        default
      )
      case false => default
    }).getOrElse(
      default
    )
  }

  /**
    * Defines the URL that is used after logout.
    *
    * @return
    */
  def redirectAfterLogout : Result = {
    Redirect(routes.Auth.signIn)
  }
}
