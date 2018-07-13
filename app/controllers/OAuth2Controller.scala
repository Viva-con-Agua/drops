package controllers

import play.api._
import play.api.mvc._
import play.api.i18n.{Messages, MessagesApi}
//import play.api.libs.json._
//import play.api.Play.current
//import play.api.i18n.Messages.Implicits._
import play.api.libs.json.Json

//import scala.concurrent.ExecutionContext

import play.api.libs.concurrent.Execution.Implicits._
import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import daos.{OauthClientDao, OauthCodeDao}
import models.{OauthCode, User}

import scalaoauth2.provider._
import oauth2server._

import scala.concurrent.Future

/**
  * Created by johann on 25.11.16.
  */
class OAuth2Controller @Inject() (
 oauthCodeDao: OauthCodeDao,
 oauthClientDao: OauthClientDao,
 oauthDataHandler: OAuthDataHandler,
 val messagesApi: MessagesApi,
 configuration: Configuration,
  val env:Environment[User,CookieAuthenticator]
) extends Silhouette[User,CookieAuthenticator] with OAuth2Provider {
  override val tokenEndpoint = new DropsTokenEndpoint()

  def accessToken = Action.async { implicit request =>
    issueAccessToken(oauthDataHandler) recover {
      case e: InvalidClient => generateJsonErrorMsg(request, play.api.mvc.Results.Forbidden, e.errorType, e.description)
      case e: InvalidGrant => generateJsonErrorMsg(request, play.api.mvc.Results.Forbidden, e.errorType, e.description)
      case e: InvalidScope => generateJsonErrorMsg(request, play.api.mvc.Results.Forbidden, e.errorType, e.description)
      case e: InvalidToken => generateJsonErrorMsg(request, play.api.mvc.Results.Forbidden, e.errorType, e.description)
      case e: RedirectUriMismatch => generateJsonErrorMsg(request, play.api.mvc.Results.Forbidden, e.errorType, e.description)
      case e: AccessDenied => generateJsonErrorMsg(request, play.api.mvc.Results.Forbidden, e.errorType, e.description)
      case e: UnauthorizedClient => generateJsonErrorMsg(request, play.api.mvc.Results.Unauthorized, e.errorType, e.description)
      case e: ExpiredToken => generateJsonErrorMsg(request, play.api.mvc.Results.Unauthorized, e.errorType, e.description)
      case e: InsufficientScope => generateJsonErrorMsg(request, play.api.mvc.Results.Unauthorized, e.errorType, e.description)
      case e: OAuthError => generateJsonErrorMsg(request, play.api.mvc.Results.BadRequest, e.errorType, e.description)
    }
  }

  /**
    * If a valid client was submitted, a new OAuth code will be generated and send to the clients redirect URI.
    *
    * Different possibilities to secure webservice communication are supported. First, you can use no security ('none').
    * Secondly, you can use a secret ('secret') and last the microservices can be identified using Sluice. Method in use
    * will be defined in your application.conf
    *
    * @deprecated
    * @author Johann Sell
    * @param clientId identifies the client
    * @return
    */
  def getCode(clientId : String, ajax : Option[Boolean] = None) = UserAwareAction.async { implicit request => {
    request.identity match {
      case Some(user) => oauthClientDao.find(clientId, None, "authorization_code").flatMap(_ match {
        case Some(client) => oauthCodeDao.save(OauthCode(user, client)).map(
          code => code.client.redirectUri.map((uri) => Redirect(uri + code.code)).getOrElse(
            generateJsonErrorMsg(request, play.api.mvc.Results.BadRequest, "oauth2server.clientHasNoRedirectURI")
          )
        )
        case _ => Future.successful(generateJsonErrorMsg(request, play.api.mvc.Results.BadRequest, "oauth2server.clientId.notFound"))
      })
      case _ => ajax match {
        case Some(flag) if flag => Future.successful(
          generateJsonErrorMsg(request, play.api.mvc.Results.Unauthorized, "error.oauth2.not.authenticated", Map("oauth2_client" -> clientId))
        )
        case _ => Future.successful(Redirect(routes.Auth.signIn))
      }
    }
  }}

  /**
    * If a valid client was submitted, a new OAuth code will be generated and send to the clients redirect URI.
    *
    * Different possibilities to secure webservice communication are supported. First, you can use no security ('none').
    * Secondly, you can use a secret ('secret') and last the microservices can be identified using Sluice. Method in use
    * will be defined in your application.conf
    *
    * TODO: Use Scope, extend response type
    *
    * @author Johann Sell
    * @param scope defines the set of visible attributes
    * @param client_id identifies the client
    * @param response_type explicitly defines, if a code is wanted
    * @param state can be used for stateful interaction (e.g. redirect infos; see OAuth 2 Spec)
    * @param redirect_uri redirect_uri again
    * @return
    */
  def getCodeOAuth2Spec(scope: Option[String], client_id : String, response_type : String, state: String, redirect_uri: String, ajax : Option[Boolean] = None) =
    UserAwareAction.async { implicit request =>
      request.identity match {
        case Some(user) => response_type match {
          case "code" => oauthClientDao.find(client_id, None, "authorization_code").flatMap(_ match {
            case Some(client) => client.redirectUri.map(_ == redirect_uri).getOrElse(true) match {
              case true => oauthCodeDao.save(OauthCode(user, client)).map(
                code => code.client.redirectUri.map((uri) => {
                  val queryStringCode = "code=" + code.code
                  val queryStringState = "state=" + state
                  Redirect(uri + "?" + queryStringCode + "&" + queryStringState)
                }).getOrElse(
                  generateJsonErrorMsg(request, play.api.mvc.Results.BadRequest, "oauth2server.clientHasNoRedirectURI")
                )
              )
              case _ => Future.successful(generateJsonErrorMsg(request, play.api.mvc.Results.Forbidden, "oauth2server.redirectUriDoesNotMatch"))
            }
            case _ => Future.successful(generateJsonErrorMsg(request, play.api.mvc.Results.NotFound, "oauth2server.clientId.notFound"))
          })
          case _ => Future.successful(generateJsonErrorMsg(request, play.api.mvc.Results.BadRequest, "oauth2server.responseTypeUnsupported"))
        }
        case _ => ajax match {
          case Some(flag) if flag => Future.successful(
            generateJsonErrorMsg(request, play.api.mvc.Results.Unauthorized, "error.oauth2.not.authenticated", Map("oauth2_client" -> client_id))
          )
          case _ => Future.successful(Redirect(routes.Auth.signIn))
        }
      }
    }

  protected def generateJsonErrorMsg(request: RequestHeader, code: play.api.mvc.Results.Status, msg: String, additional: Map[String, String] = Map()) : Result =
    request.accepts("application/json") match {
      case true => code(Json.obj(
        "internal_error_code" -> (code.header.status + ".OAuth2Server"),
        "http_error_code" -> code.header.status,
        "msg_i18n" -> msg,
        "msg" -> Messages(msg),
        "additional_information" -> Json.toJson(additional)
      )).as("application/json")
      case _ => generateErrorMsg(request, code, msg, additional)
    }

  protected def generateJsonErrorMsg(request: RequestHeader, code: play.api.mvc.Results.Status, typ: String, msg: String) : Result =
    generateJsonErrorMsg(request, code, "oauth2server.generic.error", Map("type" -> typ, "description" -> msg))

  protected def generateErrorMsg(request: RequestHeader, code: play.api.mvc.Results.Status, msg: String, additional: Map[String, String] = Map()) : Result =
    code(Messages(msg))
}
