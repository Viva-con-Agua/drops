package controllers.widgets

import java.util.UUID

import javax.inject.Inject

import scala.concurrent.Future
import com.mohiva.play.silhouette.api.{Environment, SecuredErrorHandler, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import controllers.rest._
import daos.MariadbUserDao
import play.api._
import play.api.mvc._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import models.{PublicUser, User}
import services.UserService
import play.api.libs.json._
import utils.Query.QueryParserError

import scala.concurrent.ExecutionContext.Implicits.global

class Users @Inject() (
                       userService: UserService,
                       userDao: MariadbUserDao,
                       val messagesApi: MessagesApi,
                       val env:Environment[User,CookieAuthenticator],
                       configuration: Configuration) extends Silhouette[User,CookieAuthenticator] {

  def validateJson[B: Reads] = BodyParsers.parse.json.validate(_.validate[B].asEither.left.map(e => BadRequest(JsError.toJson(e))))

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

  def getUsers = SecuredAction.async(validateJson[QueryBody]) { implicit request =>
    implicit val ud = userDao
    QueryBody.asUserRequest(request.body).map(_ match {
      case Left(e : QueryParserError) => WidgetResult.Bogus(request, "widgets.users.queryParser", Nil, "Widgets.GetUsers.QueryParsingError", Json.obj("error" -> Messages("rest.api.missingFilterValue"))).getResult
      case Left(e : QueryBody.NoValuesGiven) => WidgetResult.Bogus(request, "widgets.users.noValues", Nil, "Widgets.GetUsers.NoValues", Json.obj("error" -> e.getMessage)).getResult
      case Left(e) => WidgetResult.Generic(request, play.api.mvc.Results.InternalServerError, "widgets.users.generic", Nil, "Widgets.GetUsers.Generic", Json.obj("error" -> e.getMessage)).getResult
      case Right(users) => WidgetResult.Ok(request, "widgets.user.found", Nil, "Widgets.GetUsers.Success", Json.toJson(users.map((u) => PublicUser(u)))).getResult
    })
  }

  def getCountUsers = SecuredAction.async(validateJson[QueryBody]) { implicit request =>
    implicit val ud = userDao
    QueryBody.getUsersCount(request.body).map(_ match {
      case Left(e : QueryParserError) => WidgetResult.Bogus(request, "widgets.users.queryParser", Nil, "Widgets.GetCountUsers.QueryParsingError", Json.obj("error" -> Messages("rest.api.missingFilterValue"))).getResult
      case Left(e : QueryBody.NoValuesGiven) => WidgetResult.Bogus(request, "widgets.users.noValues", Nil, "Widgets.GetCountUsers.NoValues", Json.obj("error" -> e.getMessage)).getResult
      case Left(e) => WidgetResult.Generic(request, play.api.mvc.Results.InternalServerError, "widgets.users.generic", Nil, "Widgets.GetCountUsers.Generic", Json.obj("error" -> e.getMessage)).getResult
      case Right(count) => WidgetResult.Ok(request, "widgets.count.found", Nil, "Widgets.GetCountUsers.Success", Json.obj("count" -> count)).getResult
    })
  }
}
