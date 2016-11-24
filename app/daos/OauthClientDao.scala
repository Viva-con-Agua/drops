package daos

import scala.concurrent.Future
import models.OauthClient
import play.api.Play._
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.collection.JSONCollection

/**
  * Created by johann on 24.11.16.
  */
trait OauthClientDao {
  def find(id: String) : Future[Option[OauthClient]]
  def find(id: String, secret: String) : Future[Option[OauthClient]]
  def find(id: String, secret: Option[String], grantType: String) : Future[Option[OauthClient]]
  def save(client: OauthClient) : Future[OauthClient]
  def validate(id: String, secret: Option[String], grantType: String) : Future[Boolean]
}

class MongoOauthClientDao extends OauthClientDao {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  val clients = reactiveMongoApi.db.collection[JSONCollection]("oauthClients")

  def find(id: String) =
    clients.find(Json.obj(
      "id" -> id
    )).one[OauthClient]

  def find(id: String, secret: String) =
    clients.find(Json.obj(
      "id" -> id,
      "secret" -> secret
    )).one[OauthClient]

  def find(id: String, secret: Option[String], grantType: String) = {
    val query = Json.obj(
      "id" -> id,
      "grantTypes" -> grantType // should work by mongo magic
    )
    query ++ secret.map(secretString => Json.obj("secret" -> secretString)).getOrElse(Json.obj())
    clients.find(query).one[OauthClient]
  }

  def save(client:OauthClient):Future[OauthClient] =
    clients.insert(client).map(_ => client)

  def validate(id: String, secret: Option[String], grantType: String) =
    this.find(id, secret, grantType).map(_ match {
      case Some(client) => true
      case _ => false
    })
}