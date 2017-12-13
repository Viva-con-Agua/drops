package daos

import java.util.UUID

import scala.concurrent.Future
import play.api.Play
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.db.slick.DatabaseConfigProvider
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection
import com.mohiva.play.silhouette.api.LoginInfo
import daos.schema.{LoginInfoTableDef, ProfileTableDef, SupporterTableDef, UserTableDef}
import models._
import models.User._
import models.database.ProfileDB
import play.api.db.slick.DatabaseConfigProvider
import reactivemongo.bson.BSONObjectID
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._

trait UserDao extends ObjectIdResolver with CountResolver{
//  def getObjectId(userId: UUID):Future[Option[ObjectIdWrapper]]
  def find(loginInfo:LoginInfo):Future[Option[User]]
  def find(userId:UUID):Future[Option[User]]
  def save(user:User):Future[User]
  def saveProfileImage(profile: Profile, avatar: ProfileImage): Future[User]
  def replace(user:User):Future[User]
  def confirm(loginInfo:LoginInfo):Future[User]
  def link(user:User, profile:Profile):Future[User]
  def update(profile:Profile):Future[User]
  def list : Future[List[User]]
  def listOfStubs : Future[List[UserStub]]
  def delete (userId:UUID):Future[Boolean]


  trait UserWS {
    def find(userId:UUID, queryExtension: JsObject):Future[Option[User]]
    def list(queryExtension: JsObject, limit : Int, sort: JsObject):Future[List[User]]
  }
  val ws : UserWS
}

class MongoUserDao extends UserDao {
  lazy val reactiveMongoApi = current.injector.instanceOf[ReactiveMongoApi]
  val users = reactiveMongoApi.db.collection[JSONCollection]("users")

  override def getCount: Future[Int] = users.count()

  override def getObjectId(id: UUID): Future[Option[ObjectIdWrapper]] =
    users.find(Json.obj("id" -> id), Json.obj("_id" -> 1)).one[ObjectIdWrapper]

  override def getObjectId(name: String): Future[Option[ObjectIdWrapper]] =
    users.find(Json.obj("id" -> UUID.fromString(name)), Json.obj("_id" -> 1)).one[ObjectIdWrapper]

  def find(loginInfo:LoginInfo):Future[Option[User]] = 
    users.find(Json.obj(
      "profiles.loginInfo" -> loginInfo
    )).one[User]

  def find(userId:UUID):Future[Option[User]] =
    ws.find(userId, Json.obj())
    //users.find(Json.obj("id" -> userId)).one[User]

  def save(user:User):Future[User] =
    users.insert(user).map(_ => user)

  override def saveProfileImage(profile: Profile, avatar: ProfileImage): Future[User] = {
    // filter all old LocalProfileImage references. Can be removed for issue #82!
    val oldAvatar = profile.avatar.filter(_ match {
      case pi: LocalProfileImage => false
      case _ => true
    })
    val newProfile = profile.copy(avatar = avatar +: oldAvatar)
    this.update(newProfile)
  }

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

  def delete(userId: UUID):Future[Boolean] =
    users.remove(Json.obj("id" -> userId)).map(x => x.n match {
      case 0 => false
      case 1 => true
    })


  def list = ws.list(Json.obj(), 20, Json.obj())//users.find(Json.obj()).cursor[User]().collect[List]()

  override def listOfStubs: Future[List[UserStub]] = this.getCount.flatMap(c =>
    users.find(Json.obj()).sort(Json.obj()).cursor[UserStub]().collect[List](c)
  )

  class MongoUserWS extends UserWS {
    override def find(userId: UUID, queryExtension: JsObject): Future[Option[User]] =
      users.find(Json.obj("id" -> userId) ++ queryExtension).one[User]

    override def list(queryExtension: JsObject, limit : Int, sort: JsObject): Future[List[User]] =
      users.find(queryExtension).sort(sort).cursor[User]().collect[List](limit)
  }
  val ws = new MongoUserWS()
}

class MariadbUserDao extends UserDao{
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  val users = TableQuery[UserTableDef]
  val profiles = TableQuery[ProfileTableDef]
  val loginInfos = TableQuery[LoginInfoTableDef]
  val supporters = TableQuery[SupporterTableDef]

  override def find(loginInfo: LoginInfo): Future[Option[User]] = ???

  override def find(userId: UUID): Future[Option[User]] = {
    val action = for{
      (((user, profile), supporter), loginInfo) <- (users.filter(u => u.publicId === userId)
          join profiles on (_.id === _.userId) //user.id === profile.userId
          join supporters on (_._2.id === _.profileId) //profiles.id === supporters.profileId
          join loginInfos on (_._1._2.id === _.profileId) //profiles.id === loginInfo.profileId
        )} yield(user, profile, supporter, loginInfo)

    dbConfig.db.run(action.result).map(result => {
        //foldLeft[Return Data Type](Init Value)(parameter) => function
        val userList = result.seq.foldLeft[List[User]](Nil)((userList, dbEntry) =>{
          val supporter : Supporter = dbEntry._3.toSupporter
          val loginInfo : LoginInfo = dbEntry._4.toLoginInfo
          val profile : Profile = Profile(loginInfo, dbEntry._2.confirmed, dbEntry._2.email, supporter)

          if(userList.length != 0 && userList.last.id == dbEntry._1.id){
            //tail = use all elements except the head element
            //reverse.tail.reverse = erease last element from list
            userList.reverse.tail.reverse ++ List(userList.last.copy(profiles = userList.last.profiles ++ List(profile)))
          }else{
            userList ++ List(User(dbEntry._1.publicId, List(profile), Set()))
          }
        })
        userList.headOption
    })
  }

  override def save(user: User): Future[User] = ???

  override def saveProfileImage(profile: Profile, avatar: ProfileImage): Future[User] = ???

  override def replace(user: User): Future[User] = ???

  override def confirm(loginInfo: LoginInfo): Future[User] = ???

  override def link(user: User, profile: Profile): Future[User] = ???

  override def update(profile: Profile): Future[User] = ???

  override def list: Future[List[User]] = ???

  override def listOfStubs: Future[List[UserStub]] = ???

  override def delete(userId: UUID): Future[Boolean] = ???

  override def getCount: Future[Int] = ???

  override def getObjectId(id: UUID): Future[Option[ObjectIdWrapper]] = ???

  override def getObjectId(name: String): Future[Option[ObjectIdWrapper]] = ???

  class MongoUserWS extends UserWS {
    override def find(userId: UUID, queryExtension: JsObject): Future[Option[User]] = ???

    override def list(queryExtension: JsObject, limit : Int, sort: JsObject): Future[List[User]] = ???
  }
  val ws = new MongoUserWS()
}