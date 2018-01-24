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
    * @param clientSecret secures the communication, if this method is configured.
    * @return
    */
  def getCode(clientId : String, clientSecret : String) = SecuredAction.async { implicit request => {

    def bodyWithSecret(secret : Option[String]) = oauthClientDao.find(clientId, secret, "authorization_code").flatMap(_ match {
      case Some(client) => oauthCodeDao.save(OauthCode(request.identity, client)).map(
        code => code.client.redirectUri.map((uri) => Redirect(uri + code.code)).getOrElse(
          BadRequest(Messages("oauth2server.clientHasNoRedirectURI"))
        )
      )
      case _ => Future.successful(BadRequest(Messages("oauth2server.clientId.notFound")))
    })

    configuration.getString("drops.ws.security").getOrElse("secret") match {
      case "none" => bodyWithSecret(None)
      case "secret" if clientSecret != "" => bodyWithSecret(Some(clientSecret))
      case "sluice" => {
        // TODO: Implement integration for using sluice in intra-microservice communication
        Future.successful(BadRequest(Messages("oauth2server.security.method.notImplemented", "sluice")))
      }
      case _ => Future.successful(BadRequest(Messages("oauth2server.clientSecret.missing")))
    }

  }}
}
