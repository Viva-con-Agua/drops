package controllers.webapp

import play.api.mvc._
import play.api.Play.current
import javax.inject.Inject
import scala.concurrent.Future
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
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
import akka.actor._
import services._
import models._
import daos.CrewDao
import play.api.libs.concurrent.Execution.Implicits._
import services.{UserService}
import play.api.mvc.WebSocket.FrameFormatter
import play.api.libs.iteratee._
import play.api.libs.json._
import java.util.UUID
import utils.Query.QueryParserError
import controllers.rest._

class CrewController @Inject() (
  userService: UserService,
  crewService: CrewService,
  crewDao: CrewDao,
  val messagesApi: MessagesApi,
  val env: Environment[User, CookieAuthenticator]
  ) extends Silhouette[User, CookieAuthenticator] {
    implicit val mApi = messagesApi
    
    def validateJson[B: Reads] = BodyParsers.parse.json.validate(_.validate[B].asEither.left.map(e => BadRequest(JsError.toJson(e))))
    
    def get(id: UUID) = SecuredAction.async { implicit request =>
      userService.retrieve(request.authenticator.loginInfo).flatMap {
        case None => Future.successful(WebAppResult.Unauthorized(request, "error.noAuthenticatedUser", Nil, "AuthProvider.Identity.Unauthorized", Map[String, String]()).getResult)
        case Some(user) => {
          crewService.get(id).map {
            case Some(crew) => WebAppResult.Ok(request, "crew.get", Nil, "AuthProvider.Identity.Success", Json.toJson(crew)).getResult
            case _ => WebAppResult.Bogus(request, "crew.notExist", Nil, "402", Json.toJson("")).getResult
          }   
        }
      }
    }
    def get(name: String) = SecuredAction.async { implicit request => 
      userService.retrieve(request.authenticator.loginInfo).flatMap {
        case None => Future.successful(WebAppResult.Unauthorized(request, "error.noAuthenticatedUser", Nil, "AuthProvider.Identity.Unauthorized", Map[String, String]()).getResult)
        case Some(user) => {
          crewService.get(name).map {
            case Some(crew) => WebAppResult.Ok(request, "crew.get", Nil, "AuthProvider.Identity.Success", Json.toJson(crew)).getResult
            case _ => WebAppResult.Bogus(request, "crew.notExist", Nil, "402", Json.toJson("")).getResult
          }
        }
      }
    }
    //def list(event: WebSocketEvent): WebSocketEvent = ???
    def list = SecuredAction.async(validateJson[QueryBody]) { implicit request =>
      QueryBody.asCrewsQuery(request.body) match {
      case Left(e : QueryParserError) => Future.successful(
        WebAppResult.Bogus(
          request, 
          "error.webapp.crew.queryParser", 
          Nil, 
          "", 
          Json.obj("error" -> Messages("rest.api.missingFilter.Value"))
        ).getResult)
      case Left(e : QueryBody.NoValuesGiven) => Future.successful(
         WebAppResult.Bogus(
          request, 
          "error.webapp.crew.queryParser", 
          Nil, 
          "", 
          Json.obj("error" -> Messages("rest.api.missingFilter.Value"))
        ).getResult)
      case Left(e) => Future.successful(
        WebAppResult.Generic(
          request, 
          play.api.mvc.Results.InternalServerError,
          "error.webapp.crew.noValues", 
          Nil, 
          "WebApp.list.NoValues",
          Json.obj("error" -> e.getMessage)
        ).getResult)
      case Right(converter) => try {
        crewService.list_with_statement(converter.toStatement).map((crews) =>
          WebAppResult.Ok(request, "webapp.crew.found", Nil, "WebApp.GetCrews.Success", Json.toJson(crews)).getResult
          )
      } catch {
        case e: java.sql.SQLException => {
          Future.successful(
            WebAppResult.Generic(request, play.api.mvc.Results.InternalServerError, "error.webapp.crew.sql", Nil, "Webapp.GetCrews.SQLException", Json.obj("error" -> e.getMessage)).getResult
            )
          }
        }
      }  
    }

  }
  
