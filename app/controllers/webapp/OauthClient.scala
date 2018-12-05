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
import services.UserService
import play.api.mvc.WebSocket.FrameFormatter
import play.api.libs.iteratee._
import play.api.libs.json._
import java.util.UUID

import utils.Query.QueryParserError
import controllers.rest._
import utils.authorization.WithRole

class OauthClientController @Inject() (
    oauthClientService: OauthClientService,
    userService: UserService,
    val messagesApi: MessagesApi,
    val env: Environment[User, CookieAuthenticator]
  ) extends Silhouette[User, CookieAuthenticator] {

    implicit val mApi = messagesApi

    def validateJson[B: Reads] = BodyParsers.parse.json.validate(_.validate[B].asEither.left.map(e => BadRequest(JsError.toJson(e))))

    def get(id: String) = SecuredAction(WithRole(RoleAdmin)).async { implicit request =>
      userService.retrieve(request.authenticator.loginInfo).flatMap {
        case None => Future.successful(WebAppResult.Unauthorized(request, "error.noAuthenticatedUser", Nil, "AuthProvider.Identity.Unauthorized", Map[String, String]()).getResult)
        case Some(user) => {
          oauthClientService.get(id).map {
            case Some(oauthClient) => WebAppResult.Ok(request, "oauthClient.get", Nil, "AuthProvider.Identity.Success", Json.toJson(oauthClient)).getResult
            case _ => WebAppResult.Bogus(request, "oauthClient.notExist", Nil, "402", Json.toJson("")).getResult
          }
        }
      }
    }

    def list = SecuredAction(WithRole(RoleAdmin)).async { implicit request =>
      oauthClientService.list.map((clients) =>
        WebAppResult.Ok(request, "oauthClient.list", Nil, "OAuthClient.List.Success", Json.toJson(clients)).getResult
      )
    }

    def add = SecuredAction(WithRole(RoleAdmin)).async(validateJson[OauthClient]) { implicit request =>
      oauthClientService.save(request.body).map((client) =>
        WebAppResult.Ok(request, "oauthClient.add", Nil, "OAuthClient.Add.Success", Json.toJson(client)).getResult
      )
    }
  }

