package daos

import java.util.UUID

import daos.schema.{OauthClientTableDef, OauthTokenTableDef, UserTableDef}

import scala.concurrent.Future
import models.{OauthClient, OauthToken}
import models.OauthToken._
import models.converter.OauthTokenConverter
import models.database.OauthTokenDB
import play.api.Play
import play.api.Play._
import play.api.libs.json.Json
import play.api.Play.current
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.MySQLDriver.api._

import scalaoauth2.provider.AccessToken

/**
  * Created by johann on 24.11.16.
  */
trait OauthTokenDao {
  def find(uuid: UUID) : Future[Option[OauthToken]]
  def find(uuid: UUID, oauthClient: OauthClient) : Future[Option[OauthToken]]
  def find(accessToken: AccessToken) : Future[Option[OauthToken]]
  def find(accessToken: String) : Future[Option[OauthToken]]
  def findByRefresh(refreshToken: String) : Future[Option[OauthToken]]
  def save(token: OauthToken) : Future[OauthToken]
  def update(token: OauthToken) : Future[OauthToken]
  def createOrUpdate(token: OauthToken) : Future[OauthToken]
}

class MariadbOauthTokenDao extends OauthTokenDao {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  val oauthTokens = TableQuery[OauthTokenTableDef]
  val oauthClients = TableQuery[OauthClientTableDef]
  val users = TableQuery[UserTableDef]

  def find(id: Long) : Future[Option[OauthToken]] = {
    val action = for{
      ((oauthToken, oauthClient), user) <- (oauthTokens.filter(_.id === id)
        join oauthClients on (_.clientId === _.id)
        join users on (_._1.userId === _.publicId)
        )} yield (oauthToken, oauthClient, user)

    dbConfig.db.run(action.result).map(r => OauthTokenConverter.buildOauthTokenObjectFromResult(r))
  }

  override def find(uuid: UUID) : Future[Option[OauthToken]] = {
    val action = for{
      ((oauthToken, oauthClient), user) <- (oauthTokens.filter(_.userId === uuid)
        join oauthClients on (_.clientId === _.id)
        join users on (_._1.userId === _.publicId)
      )} yield (oauthToken, oauthClient, user)

    dbConfig.db.run(action.result).map(r => OauthTokenConverter.buildOauthTokenObjectFromResult(r))
  }

  override def find(uuid: UUID, oauthClient: OauthClient) : Future[Option[OauthToken]] = {
    //ToDo: is that enough to verfiy the OAuthClient?
    val action = for{
      ((oauthToken, oauthClient), user) <- (oauthTokens.filter(_.userId === uuid)
        join oauthClients.filter(oC => oC.id === oauthClient.id && oC.secret === oauthClient.secret) on (_.clientId === _.id)
        join users on (_._1.userId === _.publicId)
        )} yield (oauthToken, oauthClient, user)

    dbConfig.db.run(action.result).map(r => OauthTokenConverter.buildOauthTokenObjectFromResult(r))
  }

  override def find(accessToken: AccessToken) : Future[Option[OauthToken]] = {
    //ToDo: is that enough to verfiy the accessToken?
    val action = for{
      ((oauthToken, oauthClient), user) <- (oauthTokens.filter(_.token === accessToken.token)
        join oauthClients on (_.clientId === _.id)
        join users on (_._1.userId === _.publicId)
        )} yield (oauthToken, oauthClient, user)

    dbConfig.db.run(action.result).map(r => OauthTokenConverter.buildOauthTokenObjectFromResult(r))
  }

  override def find(accessToken: String) : Future[Option[OauthToken]] = {
    val action = for{
      ((oauthToken, oauthClient), user) <- (oauthTokens.filter(_.token === accessToken)
        join oauthClients on (_.clientId === _.id)
        join users on (_._1.userId === _.publicId)
        )} yield (oauthToken, oauthClient, user)

    dbConfig.db.run(action.result).map(r => OauthTokenConverter.buildOauthTokenObjectFromResult(r))
  }
  override def findByRefresh(refreshToken: String) : Future[Option[OauthToken]] = {
    val action = for {
      ((oauthToken, oauthClient), user) <- (oauthTokens.filter(_.refreshToken === refreshToken)
        join oauthClients on (_.clientId === _.id)
        join users on (_._1.userId === _.publicId)
        )} yield (oauthToken, oauthClient, user)

    dbConfig.db.run(action.result).map(r => OauthTokenConverter.buildOauthTokenObjectFromResult(r))
  }

  def findDBModel(accessToken: AccessToken) : Future[Option[OauthTokenDB]] = {
    dbConfig.db.run(oauthTokens.filter(_.token === accessToken.token).result).map(_.headOption)
  }

  override def save(token: OauthToken) : Future[OauthToken] = createOrUpdate(token)

  override def update(token: OauthToken) : Future[OauthToken] = createOrUpdate(token)
  
  override def createOrUpdate(token: OauthToken) : Future[OauthToken] = {
    findDBModel(token.accessToken).flatMap(t => t.isDefined match {
      case true => dbConfig.db.run(oauthTokens.filter(_.id === t.get.id).update(OauthTokenDB(token, t.get.id))).flatMap(_ => find(t.get.id))
      case false => dbConfig.db.run((oauthTokens returning oauthTokens.map(_.id)) += OauthTokenDB(token)).flatMap(r => {find(r)})
    }).map(_.get)
  }
}
