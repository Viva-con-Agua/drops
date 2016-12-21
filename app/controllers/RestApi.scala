package controllers

import java.util.UUID
import javax.inject.Inject

import scala.concurrent.Future
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import models.{OauthClient, PublicUser, User}
import User._
import api.ApiAction
import daos.{CrewDao, OauthClientDao, UserDao}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

class RestApi @Inject() (
  val userDao : UserDao,
  val crewDao: CrewDao,
  val oauthClientDao : OauthClientDao,
  val ApiAction : ApiAction,
  val messagesApi: MessagesApi,
  val env:Environment[User,CookieAuthenticator]) extends Silhouette[User,CookieAuthenticator] {

  def profile = SecuredAction.async { implicit request =>
    val json = Json.toJson(request.identity.profileFor(request.authenticator.loginInfo).get)
    val prunedJson = json.transform(
      (__ \ 'loginInfo).json.prune andThen 
      (__ \ 'passordInfo).json.prune andThen 
      (__ \ 'oauth1Info).json.prune)
    prunedJson.fold(
      _ => Future.successful(InternalServerError(Json.obj("error" -> Messages("error.profileError")))),
      js => Future.successful(Ok(js))
    )
  }

  override def onNotAuthenticated(request:RequestHeader) = {
    Some(Future.successful(Unauthorized(Json.obj("error" -> Messages("error.profileUnauth")))))
  }

  def users = ApiAction.async { implicit request =>
    userDao.list.map(users => Ok(Json.toJson(users.map(PublicUser(_)))))
  }

  def user(id : String) = ApiAction.async { implicit request =>
    userDao.find(UUID.fromString(id)).map(_ match {
      case Some(user) => Ok(Json.toJson(PublicUser(user)))
      case _ => BadRequest(Json.obj("error" -> Messages("rest.api.canNotFindGivenUser", id)))
    })
  }

  def crews = ApiAction.async { implicit request =>
    crewDao.list.map((crews) => Ok(Json.toJson(crews)))
  }
}
