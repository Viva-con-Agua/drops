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
import akka.actor.ActorRef
import daos.CrewDao
import play.api.libs.concurrent.Execution.Implicits._
import services.{UserService}
import play.api.mvc.WebSocket.FrameFormatter
import play.api.libs.iteratee._
import play.api.libs.json._
import java.util.UUID
import utils.Query.QueryParserError
import controllers.rest._
import play.api.libs.concurrent.InjectedActorSupport
import scala.util.control.NonFatal

class OauthClientSocketController @Inject() (
  oauthClientService: OauthClientService,
  val messagesApi: MessagesApi,
   val env: Environment[User, CookieAuthenticator]
  ) extends Silhouette[User, CookieAuthenticator] {
    implicit val mApi = messagesApi
   
  //WebSocket connection input and output are WebSocketEvent
  def socket = WebSocket.tryAcceptWithActor[WebSocketEvent, WebSocketEvent] { request =>
    implicit val req = Request(request, AnyContentAsEmpty)
    SecuredRequestHandler { securedRequest =>
      Future.successful(HandlerResult(Ok, Some(securedRequest.identity)))
    }.map {
      case HandlerResult(r, Some(user)) => Right(OauthClientSocketActor.props(_))
      case HandlerResult(r, None) => Left(r)
    }
  }

  object OauthClientSocketActor {
    def props(out: ActorRef) = Props(new OauthClientSocketActor(out))
  }
  
  class OauthClientSocketActor (out: ActorRef)extends WebSocketActor(out) {
    
    implicit val socketModel: String = "OauthClient"

    override def insert(event: WebSocketEvent): Future[WebSocketEvent] = {
      event.query match {
        //
        case Some(query) => {
            val firstElement = query.headOption.getOrElse(return Future.successful(WebSocketEvent("ERROR", None, Option(Messages("socket.error.query", socketModel)))))
            val oauthJsonResult: JsResult[OauthClient] = firstElement.validate[OauthClient]
            oauthJsonResult match {
              case s: JsSuccess[OauthClient] => {
                oauthClientService.save(s.get).flatMap{
                  case (oauthClient) => Future.successful(WebSocketEvent("SUCCESS", Option(List(Json.toJson(oauthClient))), Option(Messages("socket.success.insert", s.get.id))))
                }
              }
              case e: JsError => Future.successful(WebSocketEvent("ERROR", Option(List(JsError.toJson(e))), Option(Messages("socket.error.model", socketModel))))  
            }
          
        }
        case _ => Future.successful(WebSocketEvent("ERROR", None, Option(Messages("socket.error.query", socketModel))))
      }
    }
    //handle socket event for update crew
    override def update(event: WebSocketEvent): Future[WebSocketEvent] = {
      //check if there is a query, else return WebSocketEvent with error
      event.query match {
        case Some(query) => {
          // get first element of query. If the list is nil return WebSocketEvent error
          val firstElement = query.headOption.getOrElse(return Future.successful(WebSocketEvent("ERROR", None, Option(Messages("socket.error.query", socketModel)))))
          // validate Json as Crew. If there is no Crew return WebSocketEvent error.
          val oauthJsonResult: JsResult[OauthClient] = firstElement.validate[OauthClient]
          oauthJsonResult match {
            case s: JsSuccess[OauthClient] => {
              oauthClientService.update(s.get).flatMap{
                case (oauthClient) => Future.successful(WebSocketEvent("SUCCESS", Option(List(Json.toJson(oauthClient))), Option(Messages("socket.success.update", s.get.id))))
              }
            }
            case e: JsError => Future.successful(WebSocketEvent("ERROR", Option(List(JsError.toJson(e))), Option(Messages("socket.error.model", socketModel))))
          }
        }
        case _ => Future.successful(WebSocketEvent("ERROR", None, Option(Messages("socket.error.query", socketModel))))
      }
    }
  
    override def delete(event: WebSocketEvent): Future[WebSocketEvent] = { 
    //check if there is a query, else return WebSocketEvent with error
      event.query match {
        case Some(query) => {
          // get first element of query. If the list is nil return WebSocketEvent error
          val firstElement = query.headOption.getOrElse(return Future.successful(WebSocketEvent("ERROR", None, Option(Messages("socket.error.query", socketModel)))))
          // validate Json as Crew. If there is no Crew return WebSocketEvent error.
          val crewJsonResult: JsResult[OauthClient] = firstElement.validate[OauthClient]
          crewJsonResult match {
            case s: JsSuccess[OauthClient] => {
              oauthClientService.delete(s.get).flatMap{
                case true => Future.successful(WebSocketEvent("SUCCESS", None, Option(Messages("socket.success.delete", s.get.id))))
                case false => Future.successful(WebSocketEvent("ERROR", None, Option(Messages("socket.error.notFound", s.get.id))))
              }
            }
            case e: JsError => Future.successful(WebSocketEvent("ERROR", Option(List(JsError.toJson(e))), Option(Messages("socket.error.model", socketModel))))
          }
        }
        case _ => Future.successful(WebSocketEvent("ERROR", None, Option(Messages("socket.error.query", socketModel))))
      }
    }

    override def notMatch(event: WebSocketEvent): WebSocketEvent =  
      WebSocketEvent("ERROR", None, Option(Messages("socket.error.ops", event.operation)))
  }
}

