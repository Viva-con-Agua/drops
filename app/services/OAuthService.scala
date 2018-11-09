package services

import javax.inject._
import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import daos._
import models._
import controllers.rest.QueryBody
import utils.Nats
import slick.jdbc.SQLActionBuilder

class OauthClientService @Inject() (oauthClientDao: OauthClientDao) {
  def save(client: OauthClient): Future[OauthClient] = oauthClientDao.save(client)
  def update(client: OauthClient): Future[OauthClient] = ???
  def get(id: String): Future[Option[OauthClient]] = oauthClientDao.find(id)
  def get(id: String, secret: String): Future[Option[OauthClient]] = oauthClientDao.find(id, secret)
  def get(id: String, secret: Option[String], grandType: String): Future[Option[OauthClient]] = oauthClientDao.find(id, secret, grandType)
  def delete(client: OauthClient): Future[Boolean] = ???
  def list_with_statement(statement: SQLActionBuilder): Future[List[OauthClient]] = ???
}
