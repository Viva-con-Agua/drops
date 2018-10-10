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

class CrewSocketController @Inject() (
  crewService: CrewService,
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
      case HandlerResult(r, Some(user)) => Right(CrewWebSocketActor.props(_))
      case HandlerResult(r, None) => Left(r)
    }
  }

  object CrewWebSocketActor {
    def props(out: ActorRef) = Props(new CrewWebSocketActor(out))
  }
  
  class CrewWebSocketActor (out: ActorRef)extends Actor {
  
    def handleWebSocketEvent(msg: WebSocketEvent): WebSocketEvent = {
      //lazy val responseTimestamp = currentTime
      msg.operation match {
        case "INSERT" => insert(msg)
        case "UPDATE" => update(msg)
       // case "DELETE" => delete(msg)
        case _ => WebSocketEvent("ERROR", None, Option("Operation not suppported"))
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
            val firstElement = query.headOption.getOrElse(return WebSocketEvent("ERROR", None, Option("query is empty")))
            val crewJsonResult: JsResult[CrewStub] = firstElement.validate[CrewStub]
            crewJsonResult match {
              case s: JsSuccess[CrewStub] => {
                crewService.save(s.get)
                WebSocketEvent("SUCCESS", None, Option("save crew"))
              }
              case e: JsError => WebSocketEvent("ERROR", Option(List(JsError.toJson(e))), Option("Not a Crew object"))  
            }
          
        }
        case _ => WebSocketEvent("ERROR", None, Option("Query is not a Crew object"))
      }
  
    }
    //handle socket event for update crew
    def update(event: WebSocketEvent): WebSocketEvent = {
      //check if there is a query, else return WebSocketEvent with error
      event.query match {
        case Some(query) => {
          // get first element of query. If the list is nil return WebSocketEvent error
          val firstElement = query.headOption.getOrElse(return WebSocketEvent("ERROR", None, Option("query is empty")))
          // validate Json as Crew. If there is no Crew return WebSocketEvent error.
          val crewJsonResult: JsResult[Crew] = firstElement.validate[Crew]
          crewJsonResult match {
            case s: JsSuccess[Crew] => {
              crewService.update(s.get)
              WebSocketEvent("SUCCESS", None, Option("update crew"))
            }
            case e: JsError => WebSocketEvent("ERROR", Option(List(JsError.toJson(e))), Option("Not a Crew object"))
          }
        }
        case _ => WebSocketEvent("ERROR", None, Option("no query param is set"))
      }
    }
  }
  /*
  def delete(event: WebSocketEvent): WebSocketEvent = { 
    //check if there is a query, else return WebSocketEvent with error
    event.query match {
      case Some(query) => {
        // get first element of query. If the list is nil return WebSocketEvent error
        val firstElement = query.headOption.getOrElse(return WebSocketEvent("ERROR", None, Option("query is empty")))
        // validate Json as Crew. If there is no Crew return WebSocketEvent error.
        val crewJsonResult: JsResult[Crew] = firstElement.validate[Crew]
        crewJsonResult match {
          case s: JsSuccess[Crew] => {
            crewService.delete(s.get)
            WebSocketEvent("SUCCESS", None, Option("delete crew"))
          }
          case e: JsError => WebSocketEvent("ERROR", Option(List(JsError.toJson(e))), Option("Not a Crew object"))
        }
      }
      case _ => WebSocketEvent("ERROR", None, Option("no query param is set"))
    }
  }
  */
}
    

