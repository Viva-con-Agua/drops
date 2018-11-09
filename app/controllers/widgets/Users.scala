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
    QueryBody.asUsersQuery(request.body) match {
      case Left(e : QueryParserError) => Future.successful(
        WidgetResult.Bogus(request, "widgets.users.error.queryParser", Nil, "Widgets.GetUsers.QueryParsingError", Json.obj("error" -> e.getMessage)).getResult
      )
      case Left(e : QueryBody.NoValuesGiven) => Future.successful(
        WidgetResult.Bogus(request, "widgets.users.error.noValues", Nil, "Widgets.GetUsers.NoValues", Json.obj("error" -> e.getMessage)).getResult
      )
      case Left(e) => Future.successful(
        WidgetResult.Generic(request, play.api.mvc.Results.InternalServerError, "widgets.users.error.generic", Nil, "Widgets.GetUsers.Generic", Json.obj("error" -> e.getMessage)).getResult
      )
      case Right(converter) => try {
        userDao.list_with_statement(converter.toStatement).map((users) =>
          WidgetResult.Ok(request, "widgets.user.found", Nil, "Widgets.GetUsers.Success", Json.toJson(users.map((u) => PublicUser(u)))).getResult
        )
      } catch {
        case e: java.sql.SQLException => {
          Future.successful(
            WidgetResult.Generic(request, play.api.mvc.Results.InternalServerError, "widgets.users.error.sql", Nil, "Widgets.GetUsers.SQLException", Json.obj("error" -> e.getMessage)).getResult
          )
        }
        case e: Exception => {
          Future.successful(
            WidgetResult.Generic(request, play.api.mvc.Results.InternalServerError, "widgets.users.error.generic", Nil, "Widgets.GetUsers.Generic", Json.obj("error" -> e.getMessage)).getResult
          )
        }
      }
    }
  }

  def getCountUsers = SecuredAction.async(validateJson[QueryBody]) { implicit request =>
    implicit val ud = userDao
    QueryBody.asUsersCountQuery(request.body) match {
      case Left(e : QueryParserError) => Future.successful(
        WidgetResult.Bogus(request, "widgets.users.error.queryParser", Nil, "Widgets.GetCountUsers.QueryParsingError", Json.obj("error" -> e.getMessage)).getResult
      )
      case Left(e : QueryBody.NoValuesGiven) => Future.successful(
        WidgetResult.Bogus(request, "widgets.users.error.noValues", Nil, "Widgets.GetCountUsers.NoValues", Json.obj("error" -> e.getMessage)).getResult
      )
      case Left(e) => Future.successful(
        WidgetResult.Generic(request, play.api.mvc.Results.InternalServerError, "widgets.users.error.generic", Nil, "Widgets.GetCountUsers.Generic", Json.obj("error" -> e.getMessage)).getResult
      )
      case Right(converter) => try {
        userDao.count_with_statement(converter.toCountStatement).map((count) =>
          WidgetResult.Ok(request, "widgets.count.found", Nil, "Widgets.GetCountUsers.Success", Json.obj("count" -> count)).getResult
        ) //.map(users => Ok(Json.toJson(users)))
      } catch {
        case e: java.sql.SQLException => {
          Future.successful(
            WidgetResult.Generic(request, play.api.mvc.Results.InternalServerError, "widgets.users.error.sql", Nil, "Widgets.GetUsers.SQLException", Json.obj("error" -> e.getMessage)).getResult
          )
        }
        case e: Exception => {
          Future.successful(
            WidgetResult.Generic(request, play.api.mvc.Results.InternalServerError, "widgets.users.error.generic", Nil, "Widgets.GetUsers.Generic", Json.obj("error" -> e.getMessage)).getResult
          )
        }
      }
    }
  }
}
