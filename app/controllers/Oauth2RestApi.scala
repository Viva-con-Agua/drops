package controllers

import javax.inject.Inject

import scalaoauth2.provider.OAuth2ProviderActionBuilders._
import oauth2server.OAuthDataHandler
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.i18n.{I18nSupport,MessagesApi,Messages}
import play.api._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent._

class Oauth2RestApi @Inject()(
  val messagesApi: MessagesApi,
  oauthDataHandler: OAuthDataHandler
) extends Controller {

  def profile = AuthorizedAction(oauthDataHandler).async { request =>
    val user = request.authInfo.user.profiles.headOption
    val profilesJson = request.authInfo.user.profiles.foldLeft[Seq[JsObject]](Seq())((seq, profile) => {
      seq :+ Json.toJson(profile).transform(
        (__ \ 'loginInfo).json.prune andThen
        (__ \ 'passwordInfo).json.prune andThen
        (__ \ 'oauth1Info).json.prune
      ).getOrElse(Json.obj("error" -> Messages("error.profileNotPruned")))
    })
    Future.successful(Ok(
      Json.obj(
        "id" -> request.authInfo.user.id,
        "profiles" -> JsArray(profilesJson)
      )
    ))
  }
}
