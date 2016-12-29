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
import models.{ObjectId, ObjectIdWrapper, Profile, User}
import models.User._
import reactivemongo.bson.BSONObjectID

trait UserDao {
  def getObjectId(userId: UUID):Future[Option[ObjectIdWrapper]]
  def find(loginInfo:LoginInfo):Future[Option[User]]
  def find(userId:UUID):Future[Option[User]]
  def save(user:User):Future[User]
  def replace(user:User):Future[User]
  def confirm(loginInfo:LoginInfo):Future[User]
  def link(user:User, profile:Profile):Future[User]
  def update(profile:Profile):Future[User]
  def list : Future[List[User]]

  trait UserWS {
    def find(userId:UUID, queryExtension: JsObject):Future[Option[User]]
    def list(queryExtension: JsObject, limit : Int, sort: JsObject):Future[List[User]]
  }
  val ws : UserWS
}

class MongoUserDao extends UserDao {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  val users = reactiveMongoApi.db.collection[JSONCollection]("users")

  override def getObjectId(userId: UUID): Future[Option[ObjectIdWrapper]] =
    users.find(Json.obj("id" -> userId), Json.obj("_id" -> 1)).one[ObjectIdWrapper]

  def find(loginInfo:LoginInfo):Future[Option[User]] = 
    users.find(Json.obj(
      "profiles.loginInfo" -> loginInfo
    )).one[User]

  def find(userId:UUID):Future[Option[User]] =
    ws.find(userId, Json.obj())
    //users.find(Json.obj("id" -> userId)).one[User]

  def save(user:User):Future[User] =
    users.insert(user).map(_ => user)

  def replace(user: User): Future[User] =
    users.remove(Json.obj("id" -> user.id)).flatMap[User](wr => wr.n match {
      case 1 if wr.ok => users.insert(user).map[User](_ => user)
    })

  def confirm(loginInfo:LoginInfo):Future[User] = for {
    _ <- users.update(Json.obj(
      "profiles.loginInfo" -> loginInfo),
    Json.obj(
      "$set" -> Json.obj("profiles.$.confirmed" -> true)
    ))
    user <- find(loginInfo)
  } yield user.get

  def link(user:User, profile:Profile) = for {
    _ <- users.update(Json.obj(
      "id" -> user.id
    ), Json.obj(
      "$push" -> Json.obj("profiles" -> profile)
    ))
    user <- find(user.id)
  } yield user.get

  def update(profile:Profile) = for {
    _ <- users.update(Json.obj(
      "profiles.loginInfo" -> profile.loginInfo
    ), Json.obj(
      "$set" -> Json.obj("profiles.$" -> profile)
    ))
    user <- find(profile.loginInfo)
  } yield user.get

  def list = ws.list(Json.obj(), 20, Json.obj())//users.find(Json.obj()).cursor[User]().collect[List]()

  class MongoUserWS extends UserWS {
    override def find(userId: UUID, queryExtension: JsObject): Future[Option[User]] =
      users.find(Json.obj("id" -> userId) ++ queryExtension).one[User]

    override def list(queryExtension: JsObject, limit : Int, sort: JsObject): Future[List[User]] =
      users.find(queryExtension).sort(sort).cursor[User]().collect[List](limit)
  }
  val ws = new MongoUserWS()
}
