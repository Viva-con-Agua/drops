package daos

import java.util.UUID

import javax.inject.Inject

import scala.concurrent.Future
import play.api.{Logger, Play}
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.db.slick.DatabaseConfigProvider
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import daos.schema._
import models._
import models.User._
import models.database._
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio.DBIOAction
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._
import models.converter
import slick.jdbc.{GetResult, SQLActionBuilder}

trait UserDao extends ObjectIdResolver with CountResolver{
//  def getObjectId(userId: UUID):Future[Option[ObjectIdWrapper]]
  def find(loginInfo:LoginInfo):Future[Option[User]]
  def find(userId:UUID):Future[Option[User]]
  def save(user:User):Future[User]
  def replace(user:User):Future[User]
  def confirm(loginInfo:LoginInfo):Future[User]
  def link(user:User, profile:Profile):Future[User]
  def setRulesAccepted (id: UUID, setting: Boolean): Future[Boolean]
  def getRulesAccepted (id: UUID): Future[Boolean]
  def update(profile:Profile):Future[User]
  def list : Future[List[User]]
  def listOfStubs : Future[List[UserStub]]
  def delete (userId:UUID):Future[Boolean]
  def getProfile (email: String): Future[Option[Profile]]
  def profileListByRole(id: UUID, role: String): Future[List[Profile]]
  def profileByRole(id: UUID, role: String): Future[Option[Profile]]
  def updateSupporter(updated: Profile):Future[Option[Profile]]
  def setCrew(profile: Profile): Future[Either[Int, String]]
  def setCrew(crew: Crew, profile: Profile): Future[Either[Int, String]]
  def setCrew(crewUUID: UUID, profile: Profile): Future[Either[Int, String]]
  def removeCrew(crewUUID: UUID, profile: Profile) : Future[Either[Int, String]]
  def updateProfileEmail(email: String, profile: Profile): Future[Boolean]
  def setCrewRole(crewUUID: UUID, crewDBID: Long, role: VolunteerManager, profile: Profile): Future[Either[Int, String]]

  trait UserWS {
    def find(userId:UUID, queryExtension: JsObject):Future[Option[User]]
    def list(queryExtension: JsObject, limit : Int, sort: JsObject):Future[List[User]]
  }
  val ws : UserWS
}

class MariadbUserDao @Inject()(val crewDao: MariadbCrewDao) extends UserDao {
  val logger : Logger = Logger(this.getClass())

  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  val users = TableQuery[UserTableDef]
  val profiles = TableQuery[ProfileTableDef]
  val loginInfos = TableQuery[LoginInfoTableDef]
  val passwordInfos = TableQuery[PasswordInfoTableDef]
  val supporters = TableQuery[SupporterTableDef]
  val oauth1Infos = TableQuery[OAuth1InfoTableDef]
  val organizations = TableQuery[OrganizationTableDef]
  val profileOrganizations = TableQuery[ProfileOrganizationTableDef]
  val supporterCrews = TableQuery[SupporterCrewTableDef]
  val pool1users = TableQuery[Pool1UserTableDef]
  val oauthTokens = TableQuery[OauthTokenTableDef]

  implicit val getUserResult = GetResult(r => UserDB(r.nextLong, UUID.fromString(r.nextString), r.nextString, r.nextLong, r.nextLong, r.nextBoolean, r.nextBoolean))
  implicit val getProfileResult = GetResult(r => ProfileDB(r.nextLong, r.nextBoolean, r.nextString, r.nextLong))
  implicit val getLoginInfoResult = GetResult(r => LoginInfoDB(r.nextLong, r.nextString, r.nextString, r.nextLong))
  implicit val getPasswordInfoResult = GetResult(r => Some(PasswordInfoDB(r.nextLong, r.nextString, r.nextString, r.nextLong)))
  implicit val getSupporterInfoResult = GetResult(r => SupporterDB(r.nextLong, r.nextStringOption, r.nextStringOption, r.nextStringOption, r.nextStringOption, r.nextStringOption, r.nextLongOption, r.nextStringOption, r.nextLong))
  implicit val getSupporterCrewInfoResult = GetResult(r => Some(SupporterCrewDB(r.nextLong, r.nextLong, r.nextLong, r.nextStringOption, r.nextStringOption, r.nextLong, r.nextLong)))
  implicit val getOauth1InfoResult = GetResult(r => Some(OAuth1InfoDB(r.nextLong, r.nextString, r.nextString, r.nextLong)))

  private def toUser(users: Seq[(UserDB, ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB])]): Future[Seq[User]] = {
    Future.sequence(users.map(current => {
      addCrews(Seq((current._2, current._3, current._4, current._5, current._6, current._7))).map(_.map(tuple =>
        (current._1, tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7)
      ))
    })).map(_.foldLeft[
      Seq[(UserDB, ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[Crew])]
      ](Seq())((acc, currentList) => acc ++ currentList)).map(UserDB.read( _ ))
  }

  private def toUserProfiles(profiles: Seq[(ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB])]): Future[Seq[Profile]] = {
    addCrews(profiles).map(profiles => ProfileDB.read(profiles))
  }

  private def addCrews(profiles: Seq[(ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB])]):
    Future[Seq[(ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[Crew])]] =
    Future.sequence(profiles.map(current =>
      current._6.headOption match {
        case Some(supporterCrew) => crewDao.find(supporterCrew.crewId).map(crew =>
          (current._1, current._2, current._3, current._4, current._5, current._6, crew)
        )
        case _ => Future.successful((current._1, current._2, current._3, current._4, current._5, current._6, None))
      }
    ))

  /** Find a user object by loginInfo providerId and providerKey
    *
    * @param loginInfo
    * @return
    */
  override def find(loginInfo: LoginInfo): Future[Option[User]] = {
    val action = for{
      ((((((user, profile), supporter), loginInfo), passwordInfo), oauth1Info), supporterCrew) <- (users
        join profiles on (_.id === _.userId) //user.id === profile.userId
        join supporters on (_._2.id === _.profileId) //profiles.id === supporters.profileId
        join loginInfos.filter(lI => lI.providerId === loginInfo.providerID && lI.providerKey.toLowerCase === loginInfo.providerKey.toLowerCase)
            on (_._1._2.id === _.profileId) //profiles.id === loginInfo.profileId
        joinLeft passwordInfos on(_._1._1._2.id ===   _.profileId)
        joinLeft oauth1Infos on (_._1._1._1._2.id === _.profileId)
        joinLeft supporterCrews on (_._1._1._1._2.id === _.supporterId) //profiles.id === supporterCrews.supporterId
        )} yield(user, profile, supporter, loginInfo, passwordInfo, oauth1Info, supporterCrew)

    dbConfig.db.run(action.result).flatMap(toUser( _ )).map(_.headOption)
  }

  override def find(userId: UUID): Future[Option[User]] = {
    findDBModels(userId).flatMap(toUser( _ )).map(_.headOption)
  }

  private def find(id : Long):Future[Option[User]] = {
    val action = for{
      ((((((user, profile), supporter), loginInfo), passwordInfo), oauth1Info), supporterCrew) <- (users.filter(u => u.id === id)
        join profiles on (_.id === _.userId) //user.id === profAile.userId
        join supporters on (_._2.id === _.profileId) //profiles.id === supporters.profileId
        join loginInfos on (_._1._2.id === _.profileId) //profiles.id === loginInfo.profileId
        joinLeft passwordInfos on(_._1._1._2.id === _.profileId)
        joinLeft oauth1Infos on (_._1._1._1._2.id === _.profileId)
        joinLeft supporterCrews on (_._1._1._1._2.id === _.supporterId) //profiles.id === supporterCrews.supporterId
        )} yield(user, profile, supporter, loginInfo, passwordInfo, oauth1Info, supporterCrew)

    dbConfig.db.run(action.result).flatMap(toUser( _ )).map(_.headOption)
  }

  /**
    * Function to find an user by user id and return the database models
    * @param userId
    * @return
    */
  private def findDBModels(userId: UUID) : Future[Seq[(UserDB, ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB])]] = {
    val action = for{
      ((((((user, profile), supporter), loginInfo), passwordInfo), oauth1Info), supporterCrew) <- (users.filter(u => u.publicId === userId)
        join profiles on (_.id === _.userId) //user.id === profile.userId
        join supporters on (_._2.id === _.profileId) //profiles.id === supporters.profileId
        join loginInfos on (_._1._2.id === _.profileId) //profiles.id === loginInfo.profileId
        joinLeft passwordInfos on (_._1._1._2.id === _.profileId)
        joinLeft oauth1Infos on (_._1._1._1._2.id === _.profileId)
        joinLeft supporterCrews on (_._1._1._1._2.id === _.supporterId) //profiles.id === supporterCrews.supporterId
        )} yield(user, profile, supporter, loginInfo, passwordInfo, oauth1Info, supporterCrew)

    dbConfig.db.run(action.result)
  }

  private def findDBUserModel(userId : UUID) : Future[UserDB] = {
    dbConfig.db.run(users.filter(_.publicId === userId).result).map(r => r.head)
  }

  private def findDBUserModel(id : Long) : Future[UserDB] = {
    dbConfig.db.run(users.filter(_.id === id).result).map(r => r.head)
  }

  /**
    * Create a new user object in the database
    * @param user
    * @return
    */
  override def save(user: User): Future[User]={
    val userDBObj = UserDB(user)

    dbConfig.db.run((users returning users.map(_.id)) += userDBObj).flatMap(id => {
      findDBUserModel(id)
    }).flatMap(userObj => {
      addProfiles(userObj, user.profiles)
    })
  }


  override def replace(updatedUser: User): Future[User] = {
    findDBUserModel(updatedUser.id).flatMap(user => {
      //Delete Profiles
      val deleteProfile = profiles.filter(_.userId === user.id)
      val deleteSupporter = supporters.filter(_.profileId.in(deleteProfile.map(_.id)))
      val deleteSupporterCrew = supporterCrews.filter(_.supporterId.in(deleteSupporter.map(_.id)))
      val deleteLoginInfo = loginInfos.filter(_.profileId.in(deleteProfile.map(_.id)))
      val deletePasswordInfo = passwordInfos.filter(_.profileId.in(deleteProfile.map(_.id)))
      dbConfig.db.run(
        (deletePasswordInfo.delete andThen deleteLoginInfo.delete andThen deleteSupporter.delete andThen
          deleteSupporterCrew.delete andThen deleteProfile.delete).transactionally
      ).flatMap(_ =>
          addProfiles(user, updatedUser.profiles)
      )
    })
  }

  //ToDo: Export Profile DB Operations to ProfileDAO. This has to be protected, becaus it should not used outside the DAO Package
  /**
    * internal function to add a list of profiles for one user to the database
    *
    * @param userDB
    * @param profileObjects
    * @return
    */
  
  //def replaceProfile(profile: Profile) : Future[Option[Profile]] ={
    
  //}
  private def addProfiles(userDB : UserDB, profileObjects: List[Profile]) : Future[User] = {

    Future.sequence(profileObjects.map(profile => {
      val supporter: Supporter = profile.supporter
      val loginInfo: LoginInfo = profile.loginInfo

      val crewId : Future[Option[Long]] = supporter.crew match {
        case Some(crew) => crewDao.findDBCrewModel(crew.id).map(_.map(_.id ))
        case _ => Future.successful(None)
      }

      def supporterCrewAssignment(crewDBiD: Option[Long], s: Long, r: Option[Role]) = crewDBiD match {
        case Some(crewId) => (supporterCrews returning supporterCrews.map(_.supporterId)) += (SupporterCrewDB(s, crewId, r, None))
        case _ => DBIO.successful(false)
      }

      crewId.flatMap(crewDBiD => {
        val insertion = (for {
          p <- (profiles returning profiles.map(_.id)) += ProfileDB(profile, userDB.id)
          s <- (supporters returning supporters.map(_.id)) += SupporterDB(0, supporter, p)
          _ <- supporter.roles match {
            case list if list.nonEmpty => list.tail.foldLeft(DBIO.seq(supporterCrewAssignment(crewDBiD, s, list.headOption)))(
              (seq, role) => DBIO.seq(seq, supporterCrewAssignment(crewDBiD, s, Some(role)))
            )
            case _ => DBIO.seq(supporterCrewAssignment(crewDBiD, s, None))
          }
          _ <- (loginInfos returning loginInfos.map(_.id)) += LoginInfoDB(0, loginInfo, p)
          _ <- (profile.passwordInfo match {
            case Some(passwordInfo) => (passwordInfos returning passwordInfos.map(_.id)) += PasswordInfoDB(0, passwordInfo, p)
            case None => DBIO.successful(false)
          })
          _ <- (profile.oauth1Info match {
            case Some(oAuth1Info) => (oauth1Infos returning oauth1Infos.map(_.id)) += OAuth1InfoDB(oAuth1Info, p)
            case None => DBIO.successful(false)
          })
        } yield p).transactionally

        dbConfig.db.run(insertion)
      })
    })).flatMap(_ =>
      find(userDB.id).map(_.get)
    )
  }

  
  override def confirm(loginInfo: LoginInfo): Future[User] = {
    val getLoginInfo = loginInfos.filter(_.providerKey === loginInfo.providerKey)
    val updateProfile = for{p <- profiles.filter(_.id in getLoginInfo.map(_.profileId))} yield p.confirmed

    dbConfig.db.run((getLoginInfo.result andThen updateProfile.update(true)).transactionally).flatMap(_ => find(loginInfo)).map(_.get)
  }

  /**
    * add a new profile
    * @param user
    * @param profile
    * @return
    */
  override def link(user: User, profile: Profile): Future[User] = {
    find(profile.loginInfo).flatMap(user => {
      findDBUserModel(user.get.id).flatMap(userDB => {
          addProfiles(userDB, List(profile))
      })
    })
  }



  /**
    * replace a profile
    * @param profile
    * @return
    */
  override def update(profile: Profile): Future[User] = {
    find(profile.loginInfo).flatMap(user => {
      findDBUserModel(user.get.id).flatMap(userDB => {
        //Delete Profile
        val deleteLoginInfo = loginInfos.filter(_.providerKey === profile.loginInfo.providerKey)
        val deleteProfile = profiles.filter(_.id in (deleteLoginInfo.map(_.profileId)))
        val deleteSupporter = supporters.filter(_.profileId.in(deleteProfile.map(_.id)))
        val deleteSupporterCrew = supporterCrews.filter(_.supporterId.in(deleteSupporter.map(_.id)))
        val deletePasswordInfo = passwordInfos.filter(_.profileId.in(deleteProfile.map(_.id)))
        dbConfig.db.run(
          (deletePasswordInfo.delete andThen deleteLoginInfo.delete andThen deleteSupporterCrew.delete andThen
            deleteSupporter.delete andThen deleteProfile.delete).transactionally
        ).flatMap(_ =>
          addProfiles(userDB, List(profile))
        )
      })
    })
  }
 
  
  def updateProfileEmail(email: String, profile: Profile): Future[Boolean] = {
    val action = for {
      p <- profiles.filter(_.email === email).map(p => 
        (p.email)
      ).update((profile.email.get))
      l <- loginInfos.filter(_.providerKey === email).map(l =>
        (l.providerKey)
      ).update((profile.email.get))
      p1 <- pool1users.filter(_.email === email).map(p1 =>
          (p1.email)
      ).update((profile.email.get))
    } yield (p, l, p1)
    dbConfig.db.run((action).transactionally).map(_ match {
      case (1, 1, 1) => true
      case (1, 1, 0) => true
      case _ => false
    })
  }
  override def list: Future[List[User]] = {
    val action = for{
      ((((((user, profile), supporter), loginInfo),passwordInfo), oauth1Info), supporterCrew) <- (users
        join profiles on (_.id === _.userId) //user.id === profile.userId
        join supporters on (_._2.id === _.profileId) //profiles.id === supporters.profileId
        join loginInfos on (_._1._2.id === _.profileId) //profiles.id === loginInfo.profileId
        joinLeft passwordInfos on(_._1._1._2.id === _.profileId)
        joinLeft oauth1Infos on (_._1._1._1._2.id === _.profileId)
        joinLeft supporterCrews on (_._1._1._1._2.id === _.supporterId) //profiles.id === supporterCrews.supporterId
        )} yield(user, profile, supporter, loginInfo, passwordInfo, oauth1Info, supporterCrew)

//    dbConfig.db.run(action.result)
    dbConfig.db.run(action.result).flatMap(toUser( _ )).map(_.toList)
  }

  def list_with_statement(statement: SQLActionBuilder) : Future[List[User]] = {
    val sql_action = statement.as[(UserDB, ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB])]
    dbConfig.db.run(sql_action).flatMap(toUser( _ )).map(_.toList)
  }

  def count_with_statement(statement : SQLActionBuilder) : Future[Long] = {
    val sql_action = statement.as[Long]
    dbConfig.db.run(sql_action).map(_.head)
  }

  override def listOfStubs: Future[List[UserStub]] = {
    list.map(userList => {
      userList.map(_.toUserStub)
    })
  }


  override def delete(userId: UUID): Future[Boolean] = {
    val deleteUser = users.filter(u => u.publicId === userId)
    val deleteProfile = profiles.filter(p => p.userId.in(deleteUser.map(_.id)) )
    val deleteSupporter = supporters.filter(_.profileId.in(deleteProfile.map(_.id)))
    val deleteSupporterCrew = supporterCrews.filter(_.supporterId.in(deleteSupporter.map(_.id)))
    val deletePasswordInfo = passwordInfos.filter(_.profileId.in(deleteProfile.map(_.id)))
    val deleteLoginInfo = loginInfos.filter(_.profileId.in(deleteProfile.map(_.id)))
    val deleteOauthToken = oauthTokens.filter(t => t.userId === userId)
    dbConfig.db.run((deleteSupporterCrew.delete andThen deleteSupporter.delete andThen deleteLoginInfo.delete andThen deletePasswordInfo.delete andThen
      deleteProfile.delete andThen deleteOauthToken.delete andThen deleteUser.delete).transactionally).map(_ match {
        case 1 => true
        case 0 => false
      })
  }

  override def getCount: Future[Int] = {
    dbConfig.db.run(users.length.result)
  }


  //Return the db id for the user
  override def getObjectId(id: UUID): Future[Option[ObjectIdWrapper]] = {
    findDBUserModel(id).map(u => {
      Option(ObjectIdWrapper(ObjectId(u.id.toString)))
    })
  }

  override def getObjectId(name: String): Future[Option[ObjectIdWrapper]] = getObjectId(UUID.fromString(name))

  def getProfile(email: String): Future[Option[Profile]] = {
    val action = for{
      (((((profile, supporter), loginInfo), passwordInfo), oauth1Info), supporterCrew) <- ( profiles.filter(p => p.email === email)
        join supporters on (_.id === _.profileId)
        join loginInfos on (_._1.id === _.profileId)
        joinLeft passwordInfos on(_._1._1.id === _.profileId)
        joinLeft oauth1Infos on (_._1._1._1.id === _.profileId)
        joinLeft supporterCrews on (_._1._1._1._2.id === _.supporterId)
      )
    } yield(profile, supporter, loginInfo, passwordInfo, oauth1Info, supporterCrew)
    dbConfig.db.run(action.result).flatMap(toUserProfiles( _ )).map(_.headOption)
  }

  def getRulesAccepted(id: UUID):Future[Boolean] = {
    findDBUserModel(id).map(_.rulesAccepted)
  }

  def setRulesAccepted(id: UUID, setting: Boolean):Future[Boolean] = {
    val action = for {
      u <- users.filter(_.publicId === id)
    } yield u.rulesAccepted
    dbConfig.db.run(action.update(setting)).map(_ match {
      case 1 => true
      case 0 => false
    })
  }
  
 def profileListByRole(id: UUID, role: String):Future[List[Profile]] = {
    val action = for {
      //o <- organizations.filter(o => o.publicId === id)
      //op <- profileOrganizations.filter(po => po.organizationId === o.id)
      (((((((organization, profileOrganization), profile), supporter), loginInfo), passwordInfo), oauth1Info), supporterCrew) <- ( organizations.filter(o => o.publicId === id)
        join profileOrganizations.filter(op => op.role === role) on (_.id === _.organizationId)
          join profiles on (_._2.profileId === _.id)
          join supporters on (_._2.id === _.profileId)
          join loginInfos on (_._1._2.id === _.profileId)
          joinLeft passwordInfos on(_._1._2.id === _.profileId)
          joinLeft oauth1Infos on (_._1._2.id === _.profileId)
          joinLeft supporterCrews on (_._1._1._2.id === _.supporterId)
        )
    } yield (profile, supporter, loginInfo, passwordInfo, oauth1Info, supporterCrew)
    dbConfig.db.run(action.result).flatMap(toUserProfiles( _ )).map(_.toList)
  }

  def profileByRole(id: UUID, role: String) : Future[Option[Profile]] =
    profileListByRole(id, role).map(_.headOption)
 
 def updateSupporter(updated: Profile):Future[Option[Profile]] = {
  val action = for { 
    p <- profiles.filter(p => p.email === updated.email)
    s <- supporters.filter(s => s.profileId === p.id)
  } yield s
  dbConfig.db.run(action.result).flatMap( result => {
    dbConfig.db.run(supporters.insertOrUpdate(SupporterDB(result.head.id, updated.supporter, result.head.profileId)))
  }).flatMap(_ =>
    setCrew(updated)
  ).flatMap(_ =>
    updated.email.map(getProfile( _ )).getOrElse(Future.successful(None))
  )
 }

//  def getSupporterCrewDB(sc: SupporterCrewDB) : Future[SupporterCrewDB] = dbConfig.db.run(
//    supporterCrews.filter((row) =>
//      row.supporterId === sc.supporterId && row.crewId === sc.crewId &&
//        ((row.role === sc.role && row.pillar === sc.pillar) || (row.role.isEmpty && row.pillar.isEmpty)))
//      .map(_.id)
//      .result
//  ).map(_.headOption match {
//    case Some(id) => sc.copy(id = id)
//    case None => sc
//  })

  /**
    * Returns left an Int indicating if the database operation was successful and how many rows are changed / added.
    * If it returns right a String, that means, there was no database operation executed, because of missing data.
    * The String contains en error message.
    *
    * @author Johann Sell
    * @param profile
    * @return
    */
  def setCrew(profile: Profile): Future[Either[Int, String]] = {
    profile.supporter.crew match {
      case Some(crew) => this.setCrew(crew.id, profile)
      case _ => Future.successful(Right("dao.user.error.notFound.crew"))
    }
  }

  def setCrew(crew: Crew, profile: Profile): Future[Either[Int, String]] = {
    this.setCrew(crew.id, profile)
  }

  def setCrew(crewUUID: UUID, profile: Profile): Future[Either[Int, String]] = {
    crewDao.findDBCrewModel(crewUUID).flatMap(_.map(crewDB => {
      val action = for {
        p <- profiles.filter(_.email === profile.email)
        s <- supporters.filter(_.profileId === p.id)
      } yield s
      dbConfig.db.run(action.result).flatMap(_.headOption match {
        case Some(supporter) => Future.sequence((SupporterCrewDB * (supporter.id, crewDB.id, profile.supporter.roles, None)).map( sc =>
          dbConfig.db.run(supporterCrews += ( sc ))
        )).map(_.foldLeft(0)((sum, i) => sum + i )).map(Left( _ ))
        case _ => Future.successful(Right("dao.user.error.notFound.supporter"))
      })
    }).getOrElse(Future.successful(Right("dao.user.error.notFound.crew"))))
  }

  def removeCrew(crewUUID: UUID, profile: Profile) : Future[Either[Int, String]] = {
    crewDao.findDBCrewModel(crewUUID).flatMap(_.map(crewDB => {
      val action = for {
        p <- profiles.filter(_.email === profile.email)
        s <- supporters.filter(_.profileId === p.id)
      } yield s
      dbConfig.db.run(action.result).flatMap(_.headOption match {
        case Some(supporter) => {
          val q = supporterCrews.filter(row => row.crewId === crewDB.id && row.supporterId === supporter.id)
          dbConfig.db.run(q.delete).map(Left( _ ))
        }
        case _ => Future.successful(Right("dao.user.error.notFound.supporter"))
      })
    }).getOrElse(Future.successful(Right("dao.user.error.notFound.crew"))))
  }

  /**
    * Assigns a role to a supporter if and onl if, the supporter has already the mentioned crew.
    * @param crewUUID
    * @param crewDBID
    * @param role
    * @param profile
    * @return
    */
  override def setCrewRole(crewUUID: UUID, crewDBID: Long, role: VolunteerManager, profile: Profile): Future[Either[Int, String]] = {
    profile.supporter.crew match {
      case Some(crew) if crew.id == crewUUID => {
        // get the SupporterDB
        val supporterQuery = (for {
          p <- profiles.filter(_.email === profile.email)
          s <- supporters.filter(_.profileId === p.id)
        } yield s)
        dbConfig.db.run(supporterQuery.result).flatMap(_.headOption match {
          case Some(supporterDB) => {
            // does the supporter already has the new role?
            def update = {
              val updateQ = for { sc <- supporterCrews if sc.supporterId === supporterDB.id && sc.crewId === crewDBID } yield (sc.role, sc.pillar)
              dbConfig.db.run(updateQ.update((Some(role.name), Some(role.getPillar.name)))).map(_ match {
                case i if i > 0 => Left(i)
                case _ => Right("dao.user.error.nothingUpdated")
              })
            }
            def insert = {
              val sc = SupporterCrewDB(supporterDB.id, crewDBID, Some(role), None )
              dbConfig.db.run(supporterCrews += sc).map(_ match {
                case i if i > 0 => Left(i)
                case _ => Right("dao.user.error.nothingUpdated")
              })
            }
            dbConfig.db.run(supporterCrews.filter(row =>
              row.supporterId === supporterDB.id && row.crewId === crewDBID && row.role.isEmpty && row.pillar.isEmpty
            ).exists.result).flatMap(_ match {
              case true => update
              case false => insert
            })
          }
          case None => Future.successful(Right("dao.user.error.notFound.supporter"))
        })
      }
      case Some(crew) => Future.successful(Right("dao.user.error.anotherCrewAssigned"))
      case None => Future.successful(Right("dao.user.error.notFound.crew"))
    }
  }

  //Wenn er private ist kann ich ihn nutzen. Ansonsten muss ich schauen wo er verwendet wird
  class MariadbUserWS extends UserWS {
    override def find(userId: UUID, queryExtension: JsObject): Future[Option[User]] = ???

    override def list(queryExtension: JsObject, limit : Int, sort: JsObject): Future[List[User]] = ???
  }
  val ws = new MariadbUserWS()
}
