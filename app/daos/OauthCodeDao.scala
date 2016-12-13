package daos

import scala.concurrent.Future
import models.{OauthCode, OauthClient}
import play.api.Play._
import play.api.libs.json.Json
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection

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