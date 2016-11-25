package oauth2server

import javax.inject._

import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider

import scala.concurrent.ExecutionContext.Implicits.global
import daos.{OauthClientDao, OauthTokenDao}
import models.{OauthToken, User}
import play.api.i18n.Messages
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import services.UserService

import scala.concurrent._
import scala.concurrent.duration.Duration
import scalaoauth2.provider._

/**
  * Created by johann on 24.11.16.
  */
class OAuthDataHandler @Inject() (
  oauthTokenDao: OauthTokenDao,
  oauthClientDao: OauthClientDao,
  userService: UserService,
  credentialsProvider: CredentialsProvider
) extends DataHandler[User] {

  def validateClient(request: AuthorizationRequest): Future[Boolean] =
    request.clientCredential match {
      case Some(credentials) => oauthClientDao.validate(credentials.clientId, credentials.clientSecret, request.grantType)
      case _ => Future.successful(false)
    }

  def findUser(request: AuthorizationRequest): Future[Option[User]] = {
    val email = request.requireParam("username")  // Todo: Parameter variable? If true -> use "email" as parameter
    val password = request.requireParam("password")
    val credentials = Credentials(email, password)
    credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
      userService.retrieve(loginInfo)
    }
  }

  def createAccessToken(authInfo: AuthInfo[User]): Future[AccessToken] = {
    authInfo.clientId match {
      case Some(clientId) => {
        Await.result(oauthClientDao.find(clientId), Duration(10000, "ms")) match {
          case Some(client) => {
            val token = OauthToken.createAccessToken(authInfo)
            oauthTokenDao.createOrUpdate(OauthToken(token, client, authInfo.user.id)).map(_.accessToken)
          }
          case _ => throw new InvalidRequest(Messages("oauth2server.clientId.notFound"))
        }
      }
      case _ => throw new InvalidRequest(Messages("oauth2server.clientId.missing"))
    }


  }

  def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[User]]] =
    oauthTokenDao.findByRefresh(refreshToken).flatMap(_ match {
      case Some(token) => userService.find(token.userId).map( _.map(
        user => AuthInfo(user, Some(token.client.id), token.accessToken.scope, Some(token.client.redirectUri))
      ))
      case _ => Future.successful(None)
    })


  /**
    * Currently we do not support Authorization Code Grant, so we don't need to implement this method.
    * @param token
    * @return
    */
  def deleteAuthCode(code: String): Future[Unit] = {
    // TODO!
    Future.successful(())
  }

  def getStoredAccessToken(authInfo: AuthInfo[User]): Future[Option[AccessToken]] =
    authInfo.clientId match {
      case Some(id) => oauthClientDao.find(id).flatMap(_ match {
        case Some(client) => oauthTokenDao.find(authInfo.user.id, client).map(_.map(_.accessToken))
        case _ => Future.successful(None)
      })
      case None => oauthTokenDao.find(authInfo.user.id).map(_.map(_.accessToken))
    }

  def refreshAccessToken(authInfo: AuthInfo[User], refreshToken: String): Future[AccessToken] =
    authInfo.clientId match {
      case Some(clientId) => {
        Await.result(oauthClientDao.find(clientId), Duration(10000, "ms")) match {
          case Some(client) => {
            val token = OauthToken.createAccessToken(authInfo, Some(refreshToken))
            oauthTokenDao.update(OauthToken(token, client, authInfo.user.id)).map(_.accessToken)
          }
          case _ => throw new InvalidRequest(Messages("oauth2server.clientId.notFound"))
        }
      }
      case _ => throw new InvalidRequest(Messages("oauth2server.clientId.missing"))
    }

  /**
    * Currently we do not support Authorization Code Grant, so we don't need to implement this method.
    * @param token
    * @return
    */
  def findAuthInfoByCode(code: String): Future[Option[AuthInfo[User]]] = {
    // TODO!
    Future.successful(None)
  }

  def findAccessToken(token: String): Future[Option[AccessToken]] =
    oauthTokenDao.find(token).map(_.map(_.accessToken))

  def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[User]]] =
    oauthTokenDao.find(accessToken).flatMap(_ match {
      case Some(token) => userService.find(token.userId).map(_.map(user =>
        AuthInfo(user, Some(token.client.id), token.accessToken.scope, Some(token.client.redirectUri))
      ))
      case _ => Future.successful(None)
    })
}
