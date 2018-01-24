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
import com.mohiva.play.silhouette.api.util.PasswordInfo
import daos.schema._
import models._
import models.User._
import models.database._
import play.api.db.slick.DatabaseConfigProvider
import reactivemongo.bson.BSONObjectID
import slick.dbio.DBIOAction
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._
import models.converter
import models.converter.UserConverter

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
  val passwordInfos = TableQuery[PasswordInfoTableDef]
  val supporters = TableQuery[SupporterTableDef]

  /** Find a user object by loginInfo providerId and providerKey
    *
    * @param loginInfo
    * @return
    */
  override def find(loginInfo: LoginInfo): Future[Option[User]] = {
    val action = for{
      (((user, profile), supporter), loginInfo) <- (users
        join profiles on (_.id === _.userId) //user.id === profile.userId
        join supporters on (_._2.id === _.profileId) //profiles.id === supporters.profileId
        join loginInfos.filter(lI => lI.providerId === loginInfo.providerID && lI.providerKey === loginInfo.providerKey)
            on (_._1._2.id === _.profileId) //profiles.id === loginInfo.profileId
        )} yield(user, profile, supporter, loginInfo)

    dbConfig.db.run(action.result).map(result => {
      UserConverter.buildUserListFromResult(result).headOption
    })
  }

  override def find(userId: UUID): Future[Option[User]] = {
    val action = for{
      (((user, profile), supporter), loginInfo) <- (users.filter(u => u.publicId === userId)
          join profiles on (_.id === _.userId) //user.id === profile.userId
          join supporters on (_._2.id === _.profileId) //profiles.id === supporters.profileId
          join loginInfos on (_._1._2.id === _.profileId) //profiles.id === loginInfo.profileId
        )} yield(user, profile, supporter, loginInfo)

    dbConfig.db.run(action.result).map(result => {
      UserConverter.buildUserListFromResult(result).headOption
    })
  }

  private def find(id : Long):Future[Option[User]] = {
    val action = for{
      (((user, profile), supporter), loginInfo) <- (users.filter(u => u.id === id)
        join profiles on (_.id === _.userId) //user.id === profile.userId
        join supporters on (_._2.id === _.profileId) //profiles.id === supporters.profileId
        join loginInfos on (_._1._2.id === _.profileId) //profiles.id === loginInfo.profileId
        )} yield(user, profile, supporter, loginInfo)

    dbConfig.db.run(action.result).map(result => {
      UserConverter.buildUserListFromResult(result).headOption
    })
  }

  /**
    * Create a new user object in the database
    * @param user
    * @return
    */
  def save(user: User): Future[User]={
    val userDBObj = UserDB(user)
    //ToDo: Refactore - function should handle a list of profiles
    val supporter: Supporter = user.profiles.head.supporter
    val loginInfo: LoginInfo = user.profiles.head.loginInfo
    val profile : Profile = user.profiles.head

    val insertion = if(user.profiles.head.passwordInfo.isEmpty) {
      val passwordInfo: PasswordInfo = user.profiles.head.passwordInfo.get
      (for {
        u <- (users returning users.map(_.id)) += userDBObj
        p <- (profiles returning profiles.map(_.id)) += ProfileDB(profile, u)
        _ <- (supporters returning supporters.map(_.id)) += SupporterDB(0, supporter, p)
        _ <- (loginInfos returning loginInfos.map(_.id)) += LoginInfoDB(0, loginInfo, p)
        _ <- (passwordInfos returning passwordInfos.map(_.id)) += PasswordInfoDB(0, passwordInfo, p)
      } yield u).transactionally
    }
    else
      (for {
        u <- (users returning users.map(_.id)) += userDBObj
        p <- (profiles returning profiles.map(_.id)) += ProfileDB(profile, u)
        _ <- (supporters returning supporters.map(_.id)) += SupporterDB(0, supporter, p)
        _ <- (loginInfos returning loginInfos.map(_.id)) += LoginInfoDB(0, loginInfo, p)
      } yield u).transactionally

    dbConfig.db.run(insertion).flatMap(userId => find(userId)).map(_.get)
  }

  override def saveProfileImage(profile: Profile, avatar: ProfileImage): Future[User] = ???

  override def replace(user: User): Future[User] = ???

  override def confirm(loginInfo: LoginInfo): Future[User] = ???

  override def link(user: User, profile: Profile): Future[User] = ???

  override def update(profile: Profile): Future[User] = ???

  override def list: Future[List[User]] = {
    val action = for{
      (((user, profile), supporter), loginInfo) <- (users
        join profiles on (_.id === _.userId) //user.id === profile.userId
        join supporters on (_._2.id === _.profileId) //profiles.id === supporters.profileId
        join loginInfos on (_._1._2.id === _.profileId) //profiles.id === loginInfo.profileId
        )} yield(user, profile, supporter, loginInfo)

    dbConfig.db.run(action.result).map(result => {
      UserConverter.buildUserListFromResult(result)
    })
  }

  override def listOfStubs: Future[List[UserStub]] = ???

  override def delete(userId: UUID): Future[Boolean] = {
      val deleteUser = users.filter(u => u.publicId === userId)

      val deleteProfile = profiles.filter(_.userId.in(deleteUser.map(_.id)))
      val deleteSupporter = supporters.filter(_.profileId.in(deleteProfile.map(_.id)))
      val deleteLoginInfo = loginInfos.filter(_.profileId.in(deleteProfile.map(_.id)))
      dbConfig.db.run((deleteLoginInfo.delete andThen deleteSupporter.delete andThen deleteProfile.delete andThen deleteUser.delete).transactionally).map(r => {
        r match {
          case 1 => true
          case 0 => false
        }
      })
  }

  override def getCount: Future[Int] = {
    dbConfig.db.run(users.length.result)
  }

  override def getObjectId(id: UUID): Future[Option[ObjectIdWrapper]] = ???

  override def getObjectId(name: String): Future[Option[ObjectIdWrapper]] = ???

  class MongoUserWS extends UserWS {
    override def find(userId: UUID, queryExtension: JsObject): Future[Option[User]] = ???

    override def list(queryExtension: JsObject, limit : Int, sort: JsObject): Future[List[User]] = ???
  }
  val ws = new MongoUserWS()
}