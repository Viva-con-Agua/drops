package daos

import javax.inject.Inject

import daos.schema.{OauthClientTableDef, OauthCodeTableDef}
import models.database.OauthCodeDB

import scala.concurrent.Future
import models.{OauthClient, OauthCode}
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
trait OauthCodeDao {
  def find(code: String) : Future[Option[OauthCode]]
  def save(code: OauthCode) : Future[OauthCode]
  def delete(code: OauthCode) : Future[Boolean]
  def delete(code: String) : Future[Boolean]
  def validate(code: String, client: OauthClient) : Future[Boolean]
}

class MongoOauthCodeDao extends OauthCodeDao {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  val codes = reactiveMongoApi.db.collection[JSONCollection]("oauthCodes")

  def find(code: String) =
    codes.find(Json.obj(
      "code" -> code
    )).one[OauthCode]

  def save(code:OauthCode) =
    codes.insert(code).map(_ => code)

  def delete(code:OauthCode) =
    codes.remove(code).map(_.ok)

  def delete(code:String) =
    codes.remove(Json.obj("code" -> code)).map(_.ok)

  def validate(code: String, client: OauthClient) =
    this.find(code).map(_ match {
      case Some(code) => code.client == client && !code.isExpired
      case _ => false
    })
}

class MariadbOauthCodeDao @Inject() (userDao:MariadbUserDao)extends OauthCodeDao{
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  val oauthCodes = TableQuery[OauthCodeTableDef]
  val oauthClients = TableQuery[OauthClientTableDef]

  override def find(code: String) : Future[Option[OauthCode]] = {
    val action = for{
      (oauthCode, oauthClient) <- ( oauthCodes.filter(_.code === code)
        join oauthClients on (_.clientId === _.id)
        )}yield(oauthCode, oauthClient)

    dbConfig.db.run(action.result).flatMap(r => {
      if(r.headOption.isDefined){
        userDao.find(r.head._1.user_id).map(u => {
          Option(OauthCode(r.head._1.code, u.get, r.head._2.toOauthClient, r.head._1.created))
        })
      }else Future(None)
    })
  }

  override def save(code: OauthCode) : Future[OauthCode] = {
    dbConfig.db.run(oauthCodes += OauthCodeDB(code)).flatMap(_ => find(code.code)).map(_.get)
  }

  override def delete(code: OauthCode) : Future[Boolean] = {
    dbConfig.db.run(oauthCodes.filter(oC => oC.code === code.code && oC.clientId === code.client.id && oC.userId === code.user.id).delete).map(_ match{
      case 0 => false
      case 1 => true
    })
  }

  override def delete(code: String) : Future[Boolean] = {
    dbConfig.db.run(oauthCodes.filter(_.code === code).delete).map(_ match{
      case 0 => false
      case 1 => true
    })
  }

  override def validate(code: String, client: OauthClient) : Future[Boolean] = {
    dbConfig.db.run(oauthCodes.filter(oC => oC.code === code && oC.clientId === client.id).result).map(r => {
        if(r.headOption.isDefined) true
        else false
      })
  }
}
