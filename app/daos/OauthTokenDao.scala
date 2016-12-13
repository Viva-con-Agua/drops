package daos

import java.util.UUID

import scala.concurrent.Future
import models.{OauthClient, OauthToken}
import models.OauthToken._
import play.api.Play._
import play.api.libs.json.Json
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection

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

class MongoOauthTokenDao extends OauthTokenDao {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  val tokens = reactiveMongoApi.db.collection[JSONCollection]("oauthTokens")

  def find(uuid: UUID) =
    tokens.find(Json.obj(
      "userId" -> uuid
    )).one[OauthToken]


  def find(uuid: UUID, oauthClient: OauthClient) =
    tokens.find(Json.obj(
      "userId" -> uuid,
      "client" -> oauthClient
    )).one[OauthToken]

  def find(accessToken: AccessToken) =
    tokens.find(Json.obj(
      "accessToken" -> accessToken
    )).one[OauthToken]

  def find(accessToken: String) =
    tokens.find(Json.obj(
      "accessToken.token" -> accessToken
    )).one[OauthToken]

  def findByRefresh(refreshToken: String) =
    tokens.find(Json.obj(
      "accessToken.refreshToken" -> refreshToken
    )).one[OauthToken]

  def save(token:OauthToken):Future[OauthToken] =
    tokens.insert(token).map(_ => token)

  def update(token:OauthToken): Future[OauthToken] = for {
    _ <- tokens.update(Json.obj(
      "userId" -> token.userId,
      "client" -> token.client
    ), Json.obj(
      "$set" -> Json.obj("accessToken" -> token.accessToken)
    ))
    updatedToken <- find(token.userId, token.client)
  } yield updatedToken.get

  def createOrUpdate(token: OauthToken): Future[OauthToken] =
    find(token.userId, token.client).flatMap(_ match {
      case Some(t) => update(token)
      case None => save(token)
    })
}