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
import play.api.libs.concurrent.Execution.Implicits._
import services.{UserService}
import play.api.mvc.WebSocket.FrameFormatter
import play.api.libs.iteratee._
import play.api.libs.json._


class CrewController @Inject() (
  userService: UserService,
  crewService: CrewService,
  val messagesApi: MessagesApi,
  val env: Environment[User, CookieAuthenticator]
  ) extends Silhouette[User, CookieAuthenticator] {
    implicit val mApi = messagesApi
    
    //def validateJson[B: Reads] = BodyParsers.parse.json.validate(_.validate[B].asEither.left.map(e => BadRequest(JsError.toJson(e))))
    


    def insertCrew(event: WebSocketEvent): WebSocketEvent = {
      event.query match {
        case Some(query) => {
            val crewJsonResult: JsResult[CrewStub] = query.head.validate[CrewStub]
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

    def webSocketEventHandler(event: WebSocketEvent): WebSocketEvent = {
      event.operation match {
        case "INSERT" => insertCrew(event)
        case _ => WebSocketEvent("ERROR", None, Option("Operation not suppported"))
      }
    }

    def socket = WebSocket.tryAccept[WebSocketEvent] { request =>
       implicit val req = Request(request, AnyContentAsEmpty)
        SecuredRequestHandler { securedRequest =>
        Future.successful(HandlerResult(Ok, Some(securedRequest.identity)))
      }.map {
        case HandlerResult(r, Some(_)) => Right({
          val (out, channel) = Concurrent.broadcast[WebSocketEvent]

          val in = Iteratee.foreach[WebSocketEvent] {
            event => 
            channel push(webSocketEventHandler(event))
          }
          (in, out)
        })
        case HandlerResult(r, None) => Left(r)
      }
    }
  }
  
