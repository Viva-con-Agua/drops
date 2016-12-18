package controllers

import javax.inject.Inject

import scala.concurrent.Future
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import play.api._
import play.api.mvc._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import models.{OauthClient, RoleAdmin, RoleEmployee, User}
import play.api.data.Form
import play.api.data.Forms._
import services.UserService
import daos.OauthClientDao
import utils.WithRole

class Application @Inject() (
  oauth2ClientDao: OauthClientDao,
  userService: UserService,
  val messagesApi: MessagesApi,
  val env:Environment[User,CookieAuthenticator],
  socialProviderRegistry: SocialProviderRegistry) extends Silhouette[User,CookieAuthenticator] {

  def index = UserAwareAction.async { implicit request =>
    Future.successful(Ok(views.html.index(request.identity, request.authenticator.map(_.loginInfo))))
  }

  def profile = SecuredAction { implicit request =>
    Ok(views.html.profile(request.identity, request.authenticator.loginInfo, socialProviderRegistry, CrewForms.geoForm))
  }

  def updateCrew = SecuredAction.async { implicit request =>
    CrewForms.geoForm.bindFromRequest.fold(
      bogusForm => Future.successful(BadRequest(views.html.profile(request.identity, request.authenticator.loginInfo, socialProviderRegistry, bogusForm))),
      crewData => {
        request.identity.profileFor(request.authenticator.loginInfo) match {
          case Some(profile) => {
            val updatedSupporter = profile.supporter.copy(crew = Some(crewData))
            val updatedProfile = profile.copy(supporter = updatedSupporter)
            userService.update(request.identity.updateProfile(updatedProfile))
            Future.successful(Redirect("/profile"))
          }
          case None =>  Future.successful(InternalServerError(Messages("crew.update.noProfileForLogin")))
        }
      }
    )
  }

  def registration = SecuredAction(WithRole(RoleAdmin) || WithRole(RoleEmployee)) { implicit request =>
    Ok(views.html.oauth2.register(request.identity, request.authenticator.loginInfo, socialProviderRegistry, OAuth2ClientForms.register))
  }

  def registerOAuth2Client = SecuredAction(WithRole(RoleAdmin) || WithRole(RoleEmployee)).async { implicit request =>
    OAuth2ClientForms.register.bindFromRequest.fold(
      bogusForm => Future.successful(BadRequest(views.html.oauth2.register(request.identity, request.authenticator.loginInfo, socialProviderRegistry, bogusForm))),
      registerData => {
        oauth2ClientDao.save(registerData.toClient)
        Future.successful(Redirect("/"))
      }
    )
  }
}

object OAuth2ClientForms {
  case class OAuth2ClientRegister(id:String, secret: String, redirectUri: Option[String], codeRedirectUri: String, grantTypes: Set[String]) {
    def toClient = OauthClient(id, secret, redirectUri, codeRedirectUri, grantTypes)
  }
  def register = Form(mapping(
    "id" -> nonEmptyText,
    "secret" -> nonEmptyText,
    "redirectUri" -> optional(text),
    "codeRedirectUri" -> nonEmptyText,
    "grantTypes" -> nonEmptyText
  )
  ((id, secret, redirectUri, codeRedirectUri, grantTypes) => OAuth2ClientRegister(id, secret, redirectUri, codeRedirectUri, grantTypes.split(",").toSet))
  ((rawData) => Some((rawData.id, rawData.secret, rawData.redirectUri, rawData.codeRedirectUri, rawData.grantTypes.mkString(",")))))
}
