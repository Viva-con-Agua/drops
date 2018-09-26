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
import models._
import play.api.libs.concurrent.Execution.Implicits._
import services.{UserService}

object CrewWebSocketActor {
  def props(out: ActorRef) = Props(new CrewWebSocketActor(out))
}

class CrewWebSocketActor(out: ActorRef) extends Actor {
  def receive = {
    case msg: String =>
      out ! (s"Hi" + msg)
  }
}


class CrewController @Inject() (
  userService: UserService,
  val messagesApi: MessagesApi,
  val env: Environment[User, CookieAuthenticator]
  ) extends Silhouette[User, CookieAuthenticator] {
    implicit val mApi = messagesApi
    
    def socket = WebSocket.tryAcceptWithActor[String, String] { request =>
      implicit val req = Request(request, AnyContentAsEmpty)
      SecuredRequestHandler { securedRequest =>
        Future.successful(HandlerResult(Ok, Some(securedRequest.identity)))
      }.map {
        case HandlerResult(r, Some(user)) => Right(CrewWebSocketActor.props _)
        case HandlerResult(r, None) => Left(r)
      }
    }
  }
  
