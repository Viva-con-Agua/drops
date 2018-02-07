package daos

import java.util.UUID

import scala.concurrent.Future
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection
import com.mohiva.play.silhouette.api.LoginInfo
import models._
import models.Pool1User
import reactivemongo.bson.BSONObjectID

trait Pool1UserDAO {
  def find(email: String): Future[Option[Pool1User]]
  def save(user: Pool1User): Future[Pool1User]
}

class MongoPool1UserDAO extends Pool1UserDAO {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  val pool1users = reactiveMongoApi.db.collection[JSONCollection]("pool1users")
  
  def find(email: String):Future[Option[Pool1User]] = 
    pool1users.find(Json.obj("email" -> email)).one[Pool1User]
  
  def save(user: Pool1User): Future[Pool1User] = 
    pool1users.insert(user).map(_ => user)
}
