package controllers

import play.api._
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global

import javax.inject.Inject

import scalaoauth2.provider.OAuth2Provider
import oauth2server._

/**
  * Created by johann on 25.11.16.
  */
class OAuth2Controller @Inject() (
 oauthDataHandler: OAuthDataHandler
) extends Controller with OAuth2Provider {
  override val tokenEndpoint = new DropsTokenEndpoint()

  def accessToken = Action.async { implicit request =>
    issueAccessToken(oauthDataHandler)
  }
}
