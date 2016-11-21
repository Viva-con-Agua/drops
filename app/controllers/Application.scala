package controllers

import javax.inject.Inject

import scala.concurrent.Future
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import play.api._
import play.api.mvc._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import models.User
import services.UserService

class Application @Inject() (
  userService: UserService,
  val messagesApi: MessagesApi,
  val env:Environment[User,CookieAuthenticator],
  socialProviderRegistry: SocialProviderRegistry) extends Silhouette[User,CookieAuthenticator] {

  def index = UserAwareAction.async { implicit request =>
    Future.successful(Ok(views.html.index(request.identity, request.authenticator.map(_.loginInfo))))
  }

  def profile = SecuredAction { implicit request =>
    Ok(views.html.profile(request.identity, request.authenticator.loginInfo, socialProviderRegistry, GeographyForms.geoForm))
  }

  def updateGeography = SecuredAction.async { implicit request =>
    GeographyForms.geoForm.bindFromRequest.fold(
      bogusForm => Future.successful(BadRequest(views.html.profile(request.identity, request.authenticator.loginInfo, socialProviderRegistry, bogusForm))),
      geoData => {
        request.identity.profileFor(request.authenticator.loginInfo) match {
          case Some(profile) => {
            val updatedSupporter = profile.supporter.copy(geography = Some(geoData))
            val updatedProfile = profile.copy(supporter = updatedSupporter)
            userService.update(request.identity.updateProfile(updatedProfile))
            Future.successful(Redirect("/profile"))
          }
          case None =>  Future.successful(InternalServerError(Messages("geography.update.noProfileForLogin")))
        }
      }
    )
  }
}
