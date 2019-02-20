package daos

import daos.schema.OauthClientTableDef

import scala.concurrent.Future
import models.OauthClient
import models.database.OauthClientDB
import play.api.Play
import play.api.Play._
import play.api.libs.json.Json
import play.api.Play.current
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.MySQLDriver.api._
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.{GetResult, PositionedParameters, SQLActionBuilder, SetParameter}
import models.converter.OauthClientConverter

/**
  * Created by johann on 24.11.16.
  */
trait OauthClientDao {
  def find(id: String) : Future[Option[OauthClient]]
  def find(id: String, secret: String) : Future[Option[OauthClient]]
  def find(id: String, secret: Option[String], grantType: String) : Future[Option[OauthClient]]
  def save(client: OauthClient) : Future[OauthClient]
  def validate(id: String, secret: Option[String], grantType: String) : Future[Boolean]
  def update(client: OauthClient) : Future[OauthClient]
  def delete(client: OauthClient) : Future[Boolean]
  def list_with_statement(statement : SQLActionBuilder) : Future[List[OauthClient]]
  def list: Future[List[OauthClient]]

}

class MariadbOauthClientDao extends OauthClientDao {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  val oauthClients = TableQuery[OauthClientTableDef]

  implicit val getOauthClient = GetResult(r => OauthClientDB(r.nextString, r.nextString, r.nextStringOption, r.nextString))

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
    dbConfig.db.run(oauthClients += OauthClientDB(client)).flatMap(_ => find(client.id)).map(_.get)
  }

  override def validate(id: String, secret: Option[String], grantType: String) : Future[Boolean] = {
    find(id, secret, grantType).map(r => {
      if(r.isDefined) true
      else false
    })
  }
  
  def update(client: OauthClient) : Future[OauthClient] = {
    dbConfig.db.run(oauthClients.update(OauthClientDB(client))).flatMap(_ => find(client.id)).map(_.get) 
  }

  def delete(client: OauthClient) : Future[Boolean] = {
    dbConfig.db.run(oauthClients.filter(o => o.id === client.id).delete).flatMap {
      case 0 => Future.successful(false)
      case _ => Future.successful(true)
    }
  }

  def list_with_statement(statement : SQLActionBuilder) : Future[List[OauthClient]] = {
    var sql_action = statement.as[(OauthClientDB)]
    dbConfig.db.run(sql_action).map(OauthClientConverter.buildListFromResult(_))
  }

  def list: Future[List[OauthClient]] =
    dbConfig.db.run(oauthClients.result).map(_.map(_.toOauthClient).toList)
}
