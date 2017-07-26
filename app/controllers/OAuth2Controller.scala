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
  val env:Environment[User,CookieAuthenticator]
) extends Silhouette[User,CookieAuthenticator] with OAuth2Provider {
  override val tokenEndpoint = new DropsTokenEndpoint()

  def accessToken = UserAwareAction.async { implicit request =>
    issueAccessToken(oauthDataHandler)
  }

  def getCode(clientId : String) = SecuredAction.async { implicit request =>
    oauthClientDao.find(clientId, None, "authorization_code").flatMap(_ match {
      case Some(client) => oauthCodeDao.save(OauthCode(request.identity, client)).map(
        code => code.client.redirectUri.map( (uri) => Redirect( uri + code.code)).getOrElse(
          BadRequest(Messages("oauth2server.clientHasNoRedirectURI"))
        )
      )
      case _ => Future.successful(BadRequest(Messages("oauth2server.clientId.notFound")))
    })
  }
}
