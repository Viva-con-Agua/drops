package controllers

import play.api._
import play.api.mvc._
import play.api.i18n.{Messages, MessagesApi}
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

import scalaoauth2.provider.OAuth2Provider
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

  def accessToken = UserAwareAction.async { implicit request =>
    issueAccessToken(oauthDataHandler)
  }

  /**
    * If a valid client was submitted, a new OAuth code will be generated and send to the clients redirect URI.
    *
    * Different possibilities to secure webservice communication are supported. First, you can use no security ('none').
    * Secondly, you can use a secret ('secret') and last the microservices can be identified using Sluice. Method in use
    * will be defined in your application.conf
    *
    * @author Johann Sell
    * @param clientId identifies the client
    * @return
    */
  def getCode(clientId : String) = SecuredAction.async { implicit request => {

    oauthClientDao.find(clientId, None, "authorization_code").flatMap(_ match {
      case Some(client) => oauthCodeDao.save(OauthCode(request.identity, client)).map(
        code => code.client.redirectUri.map((uri) => Redirect(uri + code.code)).getOrElse(
          BadRequest(Messages("oauth2server.clientHasNoRedirectURI"))
        )
      )
      case _ => Future.successful(BadRequest(Messages("oauth2server.clientId.notFound")))
    })
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
  def getCodeOAuth2Spec(scope: String, client_id : String, response_type : String, state: String, redirect_uri: String) =
    SecuredAction.async { implicit request =>
      response_type match {
        case "code" => oauthClientDao.find(client_id, None, "authorization_code").flatMap(_ match {
          case Some(client) => client.redirectUri.map(_ == redirect_uri).getOrElse(true) match {
            case true => oauthCodeDao.save(OauthCode(request.identity, client)).map(
              code => code.client.redirectUri.map((uri) => {
                val queryStringCode = "code=" + code.code
                val queryStringState = "state=" + state
                Redirect(uri + "?" + queryStringCode + "&" + queryStringState)
              }).getOrElse(
                BadRequest(Messages("oauth2server.clientHasNoRedirectURI"))
              )
            )
            case _ => Future.successful(BadRequest(Messages("oauth2server.redirectUriDoesNotMatch")))
          }
          case _ => Future.successful(BadRequest(Messages("oauth2server.clientId.notFound")))
        })
        case _ => Future.successful(BadRequest(Messages("oauth2server.responseTypeUnsupported", "code")))
      }
    }

  /**
    * Implement this to return a result when the user is not authenticated.
    *
    * As defined by RFC 2616, the status code of the response should be 401 Unauthorized.
    *
    * @param request The request header.
    * @return The result to send to the client.
    */
  override def onNotAuthenticated(request: RequestHeader): Option[Future[Result]] = {
    Some(Future.successful(Unauthorized(Json.obj("error" -> "No access"))))
  }
}
