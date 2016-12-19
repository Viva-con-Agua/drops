package controllers

import javax.inject.Inject

import scala.concurrent.Future
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import play.api._
import play.api.mvc._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import models._
import play.api.data.Form
import play.api.data.Forms._
import services.UserService
import daos.{CrewDao, OauthClientDao}
import utils.{WithAlternativeRoles, WithRole}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global

class Application @Inject() (
  oauth2ClientDao: OauthClientDao,
  userService: UserService,
  crewDao: CrewDao,
  val messagesApi: MessagesApi,
  val env:Environment[User,CookieAuthenticator],
  configuration: Configuration,
  socialProviderRegistry: SocialProviderRegistry) extends Silhouette[User,CookieAuthenticator] {

  def index = UserAwareAction.async { implicit request =>
    Future.successful(Ok(views.html.index(request.identity, request.authenticator.map(_.loginInfo))))
  }

  def profile = SecuredAction.async { implicit request =>
    crewDao.list.map(l =>
      Ok(views.html.profile(request.identity, request.authenticator.loginInfo, socialProviderRegistry, CrewForms.geoForm, l.toSet))
    )
  }

  def updateCrew = SecuredAction.async { implicit request =>
    CrewForms.geoForm.bindFromRequest.fold(
      bogusForm => crewDao.list.map(l => BadRequest(views.html.profile(request.identity, request.authenticator.loginInfo, socialProviderRegistry, bogusForm, l.toSet))),
      crewData => {
        request.identity.profileFor(request.authenticator.loginInfo) match {
          case Some(profile) => {
            crewDao.find(crewData.crewName).map( _ match {
              case Some(crew) => {
                val updatedSupporter = profile.supporter.copy(crew = Some(CrewSupporter(crew, crewData.active)))
                val updatedProfile = profile.copy(supporter = updatedSupporter)
                userService.update(request.identity.updateProfile(updatedProfile))
                Redirect("/profile")
              }
              case None => Redirect("/profile")
            })

          }
          case None =>  Future.successful(InternalServerError(Messages("crew.update.noProfileForLogin")))
        }
      }
    )
  }

  def initCrews = Action.async { request =>
    configuration.getConfigList("crews").map(_.toList.map(c =>
      crewDao.find(c.getString("name").get).map(_ match {
        case Some(crew) => crew
        case _ => crewDao.save(Crew(c.getString("name").get, c.getString("country").get, c.getStringList("cities").get.toSet))
      })
    ))
    Future.successful(Redirect("/"))
  }

  def registration = SecuredAction(WithAlternativeRoles(RoleAdmin, RoleEmployee)) { implicit request =>
    Ok(views.html.oauth2.register(request.identity, request.authenticator.loginInfo, socialProviderRegistry, OAuth2ClientForms.register))
  }

  def registerOAuth2Client = SecuredAction(WithAlternativeRoles(RoleAdmin, RoleEmployee)).async { implicit request =>
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
