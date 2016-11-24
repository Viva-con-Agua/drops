package services

import javax.inject.Inject

import scalaoauth2.provider.{AccessToken, AuthInfo, AuthorizationRequest, DataHandler}
import models.User
import daos.OauthClientDao

import scala.concurrent._

/**
  * Created by johann on 24.11.16.
  */
class OAuthDataHandler @Inject() (oauthClientDao: OauthClientDao) extends DataHandler[User] {

  def validateClient(request: AuthorizationRequest): Future[Boolean] =
    request.clientCredential match {
      case Some(credentials) => oauthClientDao.validate(credentials.clientId, credentials.clientSecret, request.grantType)
      case _ => Future.successful(false)
    }

  def findUser(request: AuthorizationRequest): Future[Option[User]] = ???

  def createAccessToken(authInfo: AuthInfo[User]): Future[AccessToken] = ???

  def getStoredAccessToken(authInfo: AuthInfo[User]): Future[Option[AccessToken]] = ???

  def refreshAccessToken(authInfo: AuthInfo[User], refreshToken: String): Future[AccessToken] = ???

  def findAuthInfoByCode(code: String): Future[Option[AuthInfo[User]]] = ???

  def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[User]]] = ???

  def deleteAuthCode(code: String): Future[Unit] = ???

  def findAccessToken(token: String): Future[Option[AccessToken]] = ???

  def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[User]]] = ???
}
