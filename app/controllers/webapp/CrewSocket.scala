package controllers.webapp

import play.api.mvc._
import play.api.Play.current
import javax.inject.Inject
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.language.postfixOps
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
  
  class CrewWebSocketActor (out: ActorRef)extends WebSocketActor(out) {
    implicit val socketModel: String = "Crew"
    
    override def insert(event: WebSocketEvent): Future[WebSocketEvent] = {
      event.query match {
        //
        case Some(query) => {
          val firstElement = query.headOption.getOrElse(return Future.successful(WebSocketEvent("ERROR", None, Option(Messages("socket.error.query", socketModel)))))
          val crewJsonResult: JsResult[CrewStub] = firstElement.validate[CrewStub]
          crewJsonResult match {
            case s: JsSuccess[CrewStub] => {
              crewService.get(s.get.name).flatMap{
                case Some(crew) => Future.successful(WebSocketEvent("ERROR", None, Option("created")))
                case _ => crewService.save(s.get).flatMap{
                  case (crew) => Future.successful(WebSocketEvent("SUCCESS", Option(List(Json.toJson(crew))), Option(Messages("socket.success.insert", s.get.name))))
                  //case _ => Future.successful(WebSocketEvent("ERROR", None, Option(Messages("socket.error.insert"))))
                }
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
          val crewJsonResult: JsResult[Crew] = firstElement.validate[Crew]
          crewJsonResult match {
            case s: JsSuccess[Crew] => {
              crewService.update(s.get).flatMap{
                case (crew) => Future.successful(WebSocketEvent("SUCCESS", Option(List(Json.toJson(crew))), Option(Messages("socket.success.update", s.get.name))))
                //case _ => Future.successful(WebSocketEvent("ERROR", None, Option(Messages("socket.error.update"))))
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
        val crewJsonResult: JsResult[Crew] = firstElement.validate[Crew]
        crewJsonResult match {
          case s: JsSuccess[Crew] => {
            crewService.delete(s.get).flatMap{
              case true => Future.successful(WebSocketEvent("SUCCESS", None, Option(Messages("socket.success.delete", s.get.name))))
              case false => Future.successful(WebSocketEvent("ERROR", None, Option(Messages("socket.error.notFound", s.get.name))))
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
    

