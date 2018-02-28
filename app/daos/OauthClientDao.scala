package daos

import daos.schema.OauthClientTableDef

import scala.concurrent.Future
import models.OauthClient
import play.api.Play
import play.api.Play._
import play.api.libs.json.Json
import play.api.Play.current
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.MySQLDriver.api._
import play.api.db.slick.DatabaseConfigProvider


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

class MariadbOauthClientDao extends OauthClientDao {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  val oauthClients = TableQuery[OauthClientTableDef]

  override def find(id: String) : Future[Option[OauthClient]] = {
    dbConfig.db.run(oauthClients.filter(_.id === id).result).map(oauthClient => {
      oauthClient.headOption.map(_.toOauthClient)
    })
  }

  override def find(id: String, secret: String) : Future[Option[OauthClient]] = {
    dbConfig.db.run(oauthClients.filter(oC => oC.id === id && oC.secret === secret).result).map(oauthClient => {
      oauthClient.headOption.map(_.toOauthClient)
    })
  }

  override def find(id: String, secret: Option[String], grantType: String) : Future[Option[OauthClient]] = {
    var action = secret.isDefined match{
      case true => oauthClients.filter(oC => oC.id === id && oC.secret === secret && oC.grantTypes.like(grantType)).result
      case false => oauthClients.filter(oC => oC.id === id && oC.grantTypes.like(grantType)).result
    }

    dbConfig.db.run(action).map(oauthClient => {
      oauthClient.headOption.map(_.toOauthClient)
    })
  }

  override def save(client: OauthClient) : Future[OauthClient] = {
    dbConfig.db.run(oauthClients += client.toOauthClientDB).flatMap(_ => find(client.id)).map(_.get)
  }

  override def validate(id: String, secret: Option[String], grantType: String) : Future[Boolean] = {
    find(id, secret, grantType).map(r => {
      if(r.isDefined) true
      else false
    })
  }
}
