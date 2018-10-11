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
  oauthService: OAuthService,
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
  
  class OauthClientSocketActor (out: ActorRef)extends Actor {
    
    implicit val socketModel: String = "OauthClient"

    def handleWebSocketEvent(msg: WebSocketEvent): WebSocketEvent = {
      //lazy val responseTimestamp = currentTime
      msg.operation match {
        case "INSERT" => insert(msg)
        case "UPDATE" => update(msg)
        case "DELETE" => delete(msg)
        case _ => WebSocketEvent("ERROR", None, Option(Messages("socket.error.ops", msg.operation)))
      }
    }
    
    def receive = {
      case request: WebSocketEvent =>
        val response = handleWebSocketEvent(request)
        out ! response
    }
  
    def insert(event: WebSocketEvent): WebSocketEvent = {
      event.query match {
        //
        case Some(query) => {
            val firstElement = query.headOption.getOrElse(return WebSocketEvent("ERROR", None, Option(Messages("socket.error.query", socketModel))))
            val oauthJsonResult: JsResult[OauthClient] = firstElement.validate[OauthClient]
            oauthJsonResult match {
              case s: JsSuccess[OauthClient] => {
                oauthService.save(s.get)
                WebSocketEvent("SUCCESS", None, Option(Messages("socket.success.insert", s.get.id)))
              }
              case e: JsError => WebSocketEvent("ERROR", Option(List(JsError.toJson(e))), Option(Messages("socket.error.model", socketModel)))  
            }
          
        }
        case _ => WebSocketEvent("ERROR", None, Option(Messages("socket.error.query", socketModel)))
      }
    }
    //handle socket event for update crew
    def update(event: WebSocketEvent): WebSocketEvent = {
      //check if there is a query, else return WebSocketEvent with error
      event.query match {
        case Some(query) => {
          // get first element of query. If the list is nil return WebSocketEvent error
          val firstElement = query.headOption.getOrElse(return WebSocketEvent("ERROR", None, Option(Messages("socket.error.query", socketModel))))
          // validate Json as Crew. If there is no Crew return WebSocketEvent error.
          val oauthJsonResult: JsResult[OauthClient] = firstElement.validate[OauthClient]
          oauthJsonResult match {
            case s: JsSuccess[OauthClient] => {
              oauthService.update(s.get)
              WebSocketEvent("SUCCESS", None, Option(Messages("socket.success.update", s.get.id)))
            }
            case e: JsError => WebSocketEvent("ERROR", Option(List(JsError.toJson(e))), Option(Messages("socket.error.model", socketModel)))
          }
        }
        case _ => WebSocketEvent("ERROR", None, Option(Messages("socket.error.query", socketModel)))
      }
    }
  
    def delete(event: WebSocketEvent): WebSocketEvent = { 
    //check if there is a query, else return WebSocketEvent with error
    event.query match {
      case Some(query) => {
        // get first element of query. If the list is nil return WebSocketEvent error
        val firstElement = query.headOption.getOrElse(return WebSocketEvent("ERROR", None, Option(Messages("socket.error.query", socketModel))))
        // validate Json as Crew. If there is no Crew return WebSocketEvent error.
        val crewJsonResult: JsResult[OauthClient] = firstElement.validate[OauthClient]
        crewJsonResult match {
          case s: JsSuccess[OauthClient] => {
            oauthService.delete(s.get)
            WebSocketEvent("SUCCESS", None, Option(Messages("socket.success.delete", s.get.id)))
          }
          case e: JsError => WebSocketEvent("ERROR", Option(List(JsError.toJson(e))), Option(Messages("socket.error.model", socketModel)))
        }
      }
      case _ => WebSocketEvent("ERROR", None, Option(Messages("socket.error.query", socketModel)))
    }
    }
  }
}

