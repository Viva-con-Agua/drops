package services

import javax.inject._
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import daos._
import models._
import utils.Nats


class OAuthService @Inject() (oauthClientDao: OauthClientDao) {
  def save(client: OauthClient): Future[OauthClient] = oauthClientDao.save(client)
  def update(client: OauthClient): Future[OauthClient] = ???
  def find(id: String): Future[Option[OauthClient]] = oauthClientDao.find(id)
  def find(id: String, secret: String): Future[Option[OauthClient]] = oauthClientDao.find(id, secret)
  def find(id: String, secret: Option[String], grandType: String): Future[Option[OauthClient]] = oauthClientDao.find(id, secret, grandType)
  def delete(client: OauthClient): Future[Boolean] = ???
}
