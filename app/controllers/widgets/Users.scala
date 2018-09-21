package controllers.widgets

import java.util.UUID

import javax.inject.Inject

import scala.concurrent.Future
import com.mohiva.play.silhouette.api.{Environment, SecuredErrorHandler, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api._
import play.api.mvc._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import models.{User, PublicUser}
import services.UserService
import play.api.libs.json.{JsPath, JsValue, Json, Reads}

import scala.concurrent.ExecutionContext.Implicits.global

class Users @Inject() (
                       userService: UserService,
                       val messagesApi: MessagesApi,
                       val env:Environment[User,CookieAuthenticator],
                       configuration: Configuration) extends Silhouette[User,CookieAuthenticator] {

  implicit val m = messagesApi
  override def onNotAuthenticated(request: RequestHeader): Option[Future[Result]] = {
    Some(Future.successful(WidgetResult.Unauthorized(request, "widgets.notAuthenticated", Nil, "Widgets.NotAuthorized", Map()).getResult))
  }

  override def onNotAuthorized(request: RequestHeader): Option[Future[Result]] = {
    Some(Future.successful(WidgetResult.Forbidden(request, "widgets.forbidden", Nil, "Widgets.Forbidden", Map()).getResult))
  }

  def getUser(uuid: String) = SecuredAction.async { implicit request =>
    implicit val m = messagesApi
    userService.find(UUID.fromString(uuid)).map(
      _.map((user) =>
        WidgetResult.Ok(request, "widgets.user.found", Nil, "Widgets.GetUser.Success", Json.toJson( PublicUser(user) )).getResult
      ).getOrElse(
        WidgetResult.NotFound(request, "widgets.user.notFound", Nil, "Widgets.GetUser.NotFound", Map("uuid" -> uuid)).getResult
      )
    )
  }


}
