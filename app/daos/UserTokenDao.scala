package daos

import java.util.UUID

import daos.schema.UserTokenTableDef

import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection
import models.UserToken
import models.database.UserTokenDB
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.MySQLDriver.api._

trait UserTokenDao {
  def find(id:UUID):Future[Option[UserToken]]
  def save(token:UserToken):Future[UserToken]
  def remove(id:UUID):Future[Unit]
}

class MongoUserTokenDao extends UserTokenDao {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  val tokens = reactiveMongoApi.db.collection[JSONCollection]("tokens")

  def find(id:UUID):Future[Option[UserToken]] = 
      tokens.find(Json.obj("id" -> id)).one[UserToken]

  def save(token:UserToken):Future[UserToken] = for {
    _ <- tokens.insert(token)
  } yield token
       
  def remove(id:UUID):Future[Unit] = for {
    _ <- tokens.remove(Json.obj("id" -> id))
  } yield ()
}

class MariadbUserTokenDao extends UserTokenDao{
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  val userTokens = TableQuery[UserTokenTableDef]

  override def find(id: UUID): Future[Option[UserToken]]= dbConfig.db.run(userTokens.filter(uT => uT.id === id).result)
    .map(r => r.headOption.map(uToken => uToken.toUserToken))

  override def save(token: UserToken):Future[UserToken] = {
    val tokenDB : UserTokenDB = UserTokenDB(token)
    dbConfig.db.run((userTokens += tokenDB).andThen(DBIO.successful(tokenDB)))
    find(token.id).map(_.get)
  }

  override def remove(id: UUID):Future[Unit] = {
    dbConfig.db.run(userTokens.filter(uT => uT.id === id).delete).map(_ => ())
  }

}
