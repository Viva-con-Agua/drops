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
  def save(user:User):Future[Option[User]]
  def replace(user:User):Future[Option[User]]
  def confirm(loginInfo:LoginInfo):Future[User]
  def link(user:User, profile:Profile):Future[Option[User]]
  def update(profile:Profile):Future[Option[User]]
  def list : Future[List[User]]
  def listOfStubs : Future[List[UserStub]]
  def delete (userId:UUID):Future[Boolean]
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
  val addresses = TableQuery[AddressTableDef]
  
  def uuidFromString(uuid: Option[String]) = {
    uuid match {
      case Some(id) => Some(UUID.fromString(id))
      case _ => None
    }
  }

  implicit val getUserResult = GetResult(r => UserDB(r.nextLong, UUID.fromString(r.nextString), r.nextString, r.nextLong, r.nextLong))
  implicit val getProfileResult = GetResult(r => ProfileDB(r.nextLong, r.nextBoolean, r.nextString, r.nextLong))
  implicit val getLoginInfoResult = GetResult(r => LoginInfoDB(r.nextLong, r.nextString, r.nextString, r.nextLong))
  implicit val getPasswordInfoResult = GetResult(r => Some(PasswordInfoDB(r.nextLong, r.nextString, r.nextString, r.nextLong)))
  implicit val getSupporterInfoResult = GetResult(r => SupporterDB(r.nextLong, r.nextStringOption, r.nextStringOption, r.nextStringOption, r.nextStringOption, r.nextStringOption, r.nextLongOption, r.nextStringOption, r.nextLong))
  implicit val getSupporterCrewInfoResult = GetResult(r => Some(SupporterCrewDB(r.nextLong, r.nextLong, r.nextLong, r.nextStringOption, r.nextStringOption, r.nextLong, r.nextLong, r.nextStringOption, r.nextLongOption)))
  implicit val getOauth1InfoResult = GetResult(r => Some(OAuth1InfoDB(r.nextLong, r.nextString, r.nextString, r.nextLong)))
  implicit val getAddressInfoResult = GetResult(r => Some(AddressDB(r.nextLong, uuidFromString(r.nextStringOption), r.nextString, r.nextStringOption, r.nextString, r.nextString, r.nextString, r.nextLong)))



  /** Find a user object by loginInfo providerId and providerKey
    *
    * @param loginInfo
    * @return
    */
  override def find(loginInfo: LoginInfo): Future[Option[User]] = {
    val action = for{
      (((((((user, profile), supporter), loginInfo), passwordInfo), oauth1Info), supporterCrew), address) <- (users
        join profiles on (_.id === _.userId) //user.id === profile.userId
        join supporters on (_._2.id === _.profileId) //profiles.id === supporters.profileId
        join loginInfos.filter(lI => lI.providerId === loginInfo.providerID && lI.providerKey.toLowerCase === loginInfo.providerKey.toLowerCase)
            on (_._1._2.id === _.profileId) //profiles.id === loginInfo.profileId
        joinLeft passwordInfos on(_._1._1._2.id ===   _.profileId)
        joinLeft oauth1Infos on (_._1._1._1._2.id === _.profileId)
        joinLeft supporterCrews on (_._1._1._1._2.id === _.supporterId) //profiles.id === supporterCrews.supporterId
        joinLeft addresses on (_._1._1._1._1._2.id === _.supporterId)
        )} yield(user, profile, supporter, loginInfo, passwordInfo, oauth1Info, supporterCrew, address)
    dbConfig.db.run(action.result).flatMap(toUser( _ )).map(_.headOption)
  }

  override def find(userId: UUID): Future[Option[User]] = {
    findDBModels(userId).flatMap(toUser( _ )).map(_.headOption)
  }

  


  /**
    * Create a new user object in the database
    * @param user
    * @return
    */
  override def save(user: User): Future[Option[User]]={
    val userDBObj = UserDB(user)

    val crewIds : Future[Map[String, Option[Long]]] = Future.sequence(user.profiles.map(profile =>
      profile.supporter.crew match {
        // if supporter has a crew, find the CrewDB.id by UUID crew.id
        case Some(crew) => crewDao.findDBCrewModel(crew.id).map(_ match {
          case Some(c) => (profile.loginInfo.providerKey, Some(c.id))
          case None => (profile.loginInfo.providerKey, None)
        })
        //else None
        case _ => Future.successful((profile.loginInfo.providerKey, None))
      }
    )).map(_.toMap)
    crewIds.flatMap(c_ids =>
      dbConfig.db.run(insertUser(userDBObj, user.profiles, c_ids))
    ).flatMap(id => find(id))
  }

  override def replace(updatedUser: User): Future[Option[User]] = {
    findDBUserModel(updatedUser.id).flatMap(user => {
      //Delete Profiles
      val deleteProfile = profiles.filter(_.userId === user.id)
      val deleteSupporter = supporters.filter(_.profileId.in(deleteProfile.map(_.id)))
      val deleteSupporterCrew = supporterCrews.filter(_.supporterId.in(deleteSupporter.map(_.id)))
      val deleteLoginInfo = loginInfos.filter(_.profileId.in(deleteProfile.map(_.id)))
      val deletePasswordInfo = passwordInfos.filter(_.profileId.in(deleteProfile.map(_.id)))
      val deleteAddress = addresses.filter(_.supporterId.in(deleteSupporter.map(_.id)))
      dbConfig.db.run(
        (deletePasswordInfo.delete andThen deleteLoginInfo.delete andThen deleteAddress.delete andThen deleteSupporterCrew.delete andThen
          deleteSupporter.delete andThen deleteProfile.delete).transactionally
      ).flatMap(_ => {
        // now insert the new profiles
        // start with retrieving the crew IDs of the assigned crews
        val crewIds: Future[Map[String, Option[Long]]] = Future.sequence(updatedUser.profiles.map(profile =>
          profile.supporter.crew match {
            // if supporter has a crew, find the CrewDB.id by UUID crew.id
            case Some(crew) => crewDao.findDBCrewModel(crew.id).map(_ match {
              case Some(c) => (profile.loginInfo.providerKey, Some(c.id))
              case None => (profile.loginInfo.providerKey, None)
            })
            //else None
            case _ => Future.successful((profile.loginInfo.providerKey, None))
          }
        )).map(_.toMap)
        crewIds.flatMap(c_ids =>
          dbConfig.db.run(insertProfiles(user.id, updatedUser.profiles, c_ids))
        ).flatMap(_ => find(user.id))
        //addProfiles(user, updatedUser.profiles)
      })
    })
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
  override def link(user: User, profile: Profile): Future[Option[User]] = {
    find(profile.loginInfo).flatMap(user => {
      findDBUserModel(user.get.id).flatMap(userDB => {
        // now insert the new profiles
        // start with retrieving the crew IDs of the assigned crews
        val crewIds : Future[Map[String, Option[Long]]] =
          profile.supporter.crew match {
            // if supporter has a crew, find the CrewDB.id by UUID crew.id
            case Some(crew) => crewDao.findDBCrewModel(crew.id).map(_ match {
              case Some(c) => Map(profile.loginInfo.providerKey -> Some(c.id))
              case None => Map(profile.loginInfo.providerKey -> None)
            })
            //else None
            case _ => Future.successful(Map(profile.loginInfo.providerKey -> None))
          }
        crewIds.flatMap(c_ids =>
          dbConfig.db.run(insertProfiles(userDB.id, List(profile), c_ids))
        ).flatMap(_ => find(userDB.id))
          //addProfiles(userDB, List(profile))
      })
    })
  }



  /**
    * replace a profile
    * @param profile
    * @return
    */
  override def update(profile: Profile): Future[Option[User]] = {
    find(profile.loginInfo).flatMap(user => {
      findDBUserModel(user.get.id).flatMap(userDB => {
        //Delete Profile
        val deleteLoginInfo = loginInfos.filter(_.providerKey === profile.loginInfo.providerKey)
        val deleteProfile = profiles.filter(_.id in (deleteLoginInfo.map(_.profileId)))
        val deleteSupporter = supporters.filter(_.profileId.in(deleteProfile.map(_.id)))
        val deleteSupporterCrew = supporterCrews.filter(_.supporterId.in(deleteSupporter.map(_.id)))
        val deletePasswordInfo = passwordInfos.filter(_.profileId.in(deleteProfile.map(_.id)))
        val deleteAddress = addresses.filter(_.supporterId.in(deleteSupporter.map(_.id)))
        dbConfig.db.run(
          (deletePasswordInfo.delete andThen deleteLoginInfo.delete andThen deleteAddress.delete andThen deleteSupporterCrew.delete andThen
            deleteSupporter.delete andThen deleteProfile.delete).transactionally
        ).flatMap(_ => {
          // now insert the new profiles
          // start with retrieving the crew IDs of the assigned crews
          val crewIds: Future[Map[String, Option[Long]]] =
          profile.supporter.crew match {
            // if supporter has a crew, find the CrewDB.id by UUID crew.id
            case Some(crew) => crewDao.findDBCrewModel(crew.id).map(_ match {
              case Some(c) => Map(profile.loginInfo.providerKey -> Some(c.id))
              case None => Map(profile.loginInfo.providerKey -> None)
            })
            //else None
            case _ => Future.successful(Map(profile.loginInfo.providerKey -> None))
          }
          crewIds.flatMap(c_ids =>
            dbConfig.db.run(insertProfiles(userDB.id, List(profile), c_ids))
          ).flatMap(_ => find(userDB.id))
          //addProfiles(userDB, List(profile))
        })
      })
    })
  }
 

  override def list: Future[List[User]] = {
    val action = for{
      (((((((user, profile), supporter), loginInfo),passwordInfo), oauth1Info), supporterCrew), address) <- (users
        join profiles on (_.id === _.userId) //user.id === profile.userId
        join supporters on (_._2.id === _.profileId) //profiles.id === supporters.profileId
        join loginInfos on (_._1._2.id === _.profileId) //profiles.id === loginInfo.profileId
        joinLeft passwordInfos on(_._1._1._2.id === _.profileId)
        joinLeft oauth1Infos on (_._1._1._1._2.id === _.profileId)
        joinLeft supporterCrews on (_._1._1._1._2.id === _.supporterId)
        joinLeft addresses on (_._1._1._1._1._2.id === _.supporterId)//profiles.id === supporterCrews.supporterId
        )} yield(user, profile, supporter, loginInfo, passwordInfo, oauth1Info, supporterCrew, address)

//    dbConfig.db.run(action.result)
    dbConfig.db.run(action.result).flatMap(toUser( _ )).map(_.toList)
  }

  def list_with_statement(statement: SQLActionBuilder) : Future[List[User]] = {
    val sql_action = statement.as[(UserDB, ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[AddressDB])]
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
    val deleteAddress = addresses.filter(_.supporterId.in(deleteSupporter.map(_.id)))
    val deletePasswordInfo = passwordInfos.filter(_.profileId.in(deleteProfile.map(_.id)))
    val deleteLoginInfo = loginInfos.filter(_.profileId.in(deleteProfile.map(_.id)))
    val deleteOauthToken = oauthTokens.filter(t => t.userId === userId)
    dbConfig.db.run((deleteSupporterCrew.delete andThen deleteAddress.delete andThen deleteSupporter.delete andThen deleteLoginInfo.delete andThen deletePasswordInfo.delete andThen
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
  
  private def find(id : Long):Future[Option[User]] = {
    val action = for{
     /* (user, profile) <- (users.filter( u => u.id === id)
      join profiles on (_.id === _.userId)
    )}  yield (user, profile)*/
  
        (((((((user, profile), supporter), loginInfo), passwordInfo), oauth1Info), supporterCrew), address) <- (users.filter(u => u.id === id)
        join profiles on (_.id === _.userId) //user.id === profAile.userId
        join supporters on (_._2.id === _.profileId) //profiles.id === supporters.profileId
        join loginInfos on (_._1._2.id === _.profileId) //profiles.id === loginInfo.profileId
        joinLeft passwordInfos on(_._1._1._2.id === _.profileId)
        joinLeft oauth1Infos on (_._1._1._1._2.id === _.profileId)
        joinLeft supporterCrews on (_._1._1._1._2.id === _.supporterId) //profiles.id === supporterCrews.supporterId
        joinLeft addresses on (_._1._1._1._1._2.id === _.supporterId)
        )} yield(user, profile, supporter, loginInfo, passwordInfo, oauth1Info, supporterCrew, address)
    
    dbConfig.db.run(action.result).flatMap(toUser( _ )).map(_.headOption)
  }


  /**
    * Function to find an user by user id and return the database models
    * @param userId
    * @return
    */
  private def findDBModels(userId: UUID) : Future[Seq[(UserDB, ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[AddressDB])]] = {
    val action = for{
      (((((((user, profile), supporter), loginInfo), passwordInfo), oauth1Info), supporterCrew), address) <- (users.filter(u => u.publicId === userId)
        join profiles on (_.id === _.userId) //user.id === profile.userId
        join supporters on (_._2.id === _.profileId) //profiles.id === supporters.profileId
        join loginInfos on (_._1._2.id === _.profileId) //profiles.id === loginInfo.profileId
        joinLeft passwordInfos on (_._1._1._2.id === _.profileId)
        joinLeft oauth1Infos on (_._1._1._1._2.id === _.profileId)
        joinLeft supporterCrews on (_._1._1._1._2.id === _.supporterId) //profiles.id === supporterCrews.supporterId
        joinLeft addresses on (_._1._1._1._1._2.id === _.supporterId)
        )} yield(user, profile, supporter, loginInfo, passwordInfo, oauth1Info, supporterCrew, address)

    dbConfig.db.run(action.result)
  }

  private def findDBUserModel(userId : UUID) : Future[UserDB] = {
    dbConfig.db.run(users.filter(_.publicId === userId).result).map(r => r.head)
  }

  private def findDBUserModel(id : Long) : Future[UserDB] = {
    dbConfig.db.run(users.filter(_.id === id).result).map(r => r.head)
  }

  /**
   * Creates a [[DBIOAction]] that inserts a complete user as a transaction that inserts into multiple columns.
   *
   * @author Johann Sell
   * @param userDB
   * @param userProfiles
   * @param crewDBids
   * @return
   */
  private def insertUser(userDB: UserDB, userProfiles: List[Profile], crewDBids: Map[String, Option[Long]]) : DBIOAction[Long,slick.dbio.NoStream,slick.dbio.Effect.Write with slick.dbio.Effect.Write with slick.dbio.Effect.Write with slick.dbio.Effect.Write with slick.dbio.Effect.Write with slick.dbio.Effect.Write with slick.dbio.Effect.Write with slick.dbio.Effect.Write with slick.dbio.Effect.Transactional with slick.dbio.Effect.Transactional] = {
    (for {
      u <- (users returning users.map(_.id)) += userDB
      _ <- insertProfiles(u, userProfiles, crewDBids)
    } yield u).transactionally
  }

  /**
   * Creates a [[DBIOAction]] that inserts all given profiles as a transaction and assigns them to a given users ID.
   *
   * @author Johann Sell
   * @param userID
   * @param userProfiles
   * @param crewDBids
   * @return
   */
  private def insertProfiles(userID: Long, userProfiles: List[Profile], crewDBids: Map[String, Option[Long]]): DBIOAction[List[Long],slick.dbio.NoStream,slick.dbio.Effect.Write with slick.dbio.Effect.Write with slick.dbio.Effect.Write with slick.dbio.Effect.Write with slick.dbio.Effect.Write with slick.dbio.Effect.Write with slick.dbio.Effect.Write with slick.dbio.Effect.Transactional] = {
    // Assign a Supporter to an Crew via SupporterCrew. Each SupporterCrew Object contains one Role.
    def supporterCrewAssignment(crewDBiD: Option[Long], s: Long, r: Option[Role]) = crewDBiD match {
      case Some(crewId) => (supporterCrews returning supporterCrews.map(_.supporterId)) += (SupporterCrewDB(s, crewId, r, None, None, None))
      case _ => DBIO.successful(false)
    }

    // Assign Address to Supporter.
    def addressAssignment(address: Option[Address], supporterId: Long) = address match {
      case Some(a) => (addresses returning addresses.map(_.id)) += AddressDB(0, a, supporterId)
      case _ => DBIO.successful(false)
    }

    DBIO.sequence(userProfiles.map(profile => (for {
      // insert Profile and return long id as p
      p <- (profiles returning profiles.map(_.id)) += ProfileDB(profile, userID)
      // insert Supporter with profile_id = p and return long id as s
      s <- (supporters returning supporters.map(_.id)) += SupporterDB(0, profile.supporter, p)
      //insert Addresses with supporter_id = s.
      _ <- profile.supporter.address match {
        // if Addresses is a list and not Empty we insert every address in the list and return a DBIO.seq with all id's
        case list if list.nonEmpty => list.tail.foldLeft(DBIO.seq(addressAssignment(list.headOption, s)))(
          (seq, address) => DBIO.seq(seq, addressAssignment(Some(address), s))
        )
        // else return Database false
        case _ => DBIO.successful(false)
      }
      // same as address for all roles
      _ <- profile.supporter.roles match {
        case list if list.nonEmpty => list.tail.foldLeft(DBIO.seq(supporterCrewAssignment(crewDBids.getOrElse(profile.loginInfo.providerKey, None), s, list.headOption)))(
          (seq, role) => DBIO.seq(seq, supporterCrewAssignment(crewDBids.getOrElse(profile.loginInfo.providerKey, None), s, Some(role)))
        )
        case _ => DBIO.seq(supporterCrewAssignment(crewDBids.getOrElse(profile.loginInfo.providerKey, None), s, None))
      }
      // insert the LoginInfo PasswordInfo and OAuthInfo Models
      _ <- (loginInfos returning loginInfos.map(_.id)) += LoginInfoDB(0, profile.loginInfo, p)
      _ <- (profile.passwordInfo match {
        case Some(passwordInfo) => (passwordInfos returning passwordInfos.map(_.id)) += PasswordInfoDB(0, passwordInfo, p)
        case None => DBIO.successful(false)
      })
      _ <- (profile.oauth1Info match {
        case Some(oAuth1Info) => (oauth1Infos returning oauth1Infos.map(_.id)) += OAuth1InfoDB(oAuth1Info, p)
        case None => DBIO.successful(false)
      })
    } yield p).transactionally))
  }

  //ToDo: Export Profile DB Operations to ProfileDAO. This has to be protected, becaus it should not used outside the DAO Package
  /**
    * internal function to add a list of profiles for one user to the database
    *
    * @param userDB
    * @param profileObjects
    * @return
    */

  private def addProfiles(userDB : UserDB, profileObjects: List[Profile]) : Future[User] = {
  
    Future.sequence(profileObjects.map(profile => {

      // get Supporter and LoginInfo from Profile
      val supporter: Supporter = profile.supporter
      val loginInfo: LoginInfo = profile.loginInfo

      // get CrewDB.id by supporter.crew.id 
      val crewId : Future[Option[Long]] = supporter.crew match {
        // if supporter has a crew, find the CrewDB.id by UUID crew.id 
        case Some(crew) => crewDao.findDBCrewModel(crew.id).map(_.map(_.id ))
        //else None
        case _ => Future.successful(None)
      }
      
      // Assign a Supporter to an Crew via SupporterCrew. Each SupporterCrew Object contains one Role. 
      def supporterCrewAssignment(crewDBiD: Option[Long], s: Long, r: Option[Role]) = crewDBiD match {
        case Some(crewId) => (supporterCrews returning supporterCrews.map(_.supporterId)) += (SupporterCrewDB(s, crewId, r, None, None, None))
        case _ => DBIO.successful(false)
      }
      
      // Assign Address to Supporter.
      def addressAssignment(address: Option[Address], supporterId: Long) = address match {
        case Some(a) => (addresses returning addresses.map(_.id)) += AddressDB(0, a, supporterId)
        case _ => DBIO.successful(false)
      }

      crewId.flatMap(crewDBiD => {
        //define the insert statements
        val insertion = (for {
          
          // insert Profile and return long id as p
          p <- (profiles returning profiles.map(_.id)) += ProfileDB(profile, userDB.id)
          
          // insert Supporter with profile_id = p and return long id as s
          s <- (supporters returning supporters.map(_.id)) += SupporterDB(0, supporter, p)
          
          //insert Addresses with supporter_id = s. 
          _ <- supporter.address match {
            
            // if Addresses is a list and not Empty we insert every address in the list and return a DBIO.seq with all id's
            case list if list.nonEmpty => list.tail.foldLeft(DBIO.seq(addressAssignment(list.headOption, s)))(
                (seq, address) => DBIO.seq(seq, addressAssignment(Some(address), s))
              )
            // else return Database false
            case _ => DBIO.successful(false)
          }
          
          // same as address for all roles
          _ <- supporter.roles match {
            case list if list.nonEmpty => list.tail.foldLeft(DBIO.seq(supporterCrewAssignment(crewDBiD, s, list.headOption)))(
              (seq, role) => DBIO.seq(seq, supporterCrewAssignment(crewDBiD, s, Some(role)))
            )
            case _ => DBIO.seq(supporterCrewAssignment(crewDBiD, s, None))
          }

          // insert the LoginInfo PasswordInfo and OAuthInfo Models
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
        
        //run the insert statements
        dbConfig.db.run(insertion)
      })
    })).flatMap(_ =>
      // return user id and use the find methode to get the User
      find(userDB.id).map(_.get)
    )
  }

  /** Get a Seq[(Option[SupporterCrewDB])] and get each Crew by SupporterCrewDB.crewId in the Seq().
   * Pair the SupporterCrewDB and Crew models and return all in a Seq[(Option[SupporterCrewDB], Option[Crew])]
   *
   * @param supporterCrew
   * @return
   *
   */
  private def getCrew(supporterCrew: Seq[(Option[SupporterCrewDB])]):  Future[Seq[(Option[SupporterCrewDB], Option[Crew])]] = {
    Future.sequence(  //open Future.sequence

      //map supporterCrew: Seq[(Option[SupporterCrewDB])] to current: Option[SupporterCrewDB]
      supporterCrew.map(current => { 
      
        // match the Option[SupporterCrewDB]
        current.headOption match { //match headOption 
        
          //case Some(entry) => search crew and return (Option[SupporterCrewDB], Option[Crew]) tuple
          case Some(entry) => crewDao.find(entry.crewId).map(crew => (current, crew )) //
          
          //else the return current as (Option[SupporterCrewDB], Option[Crew]) with (None, None) 
          case _ => Future.successful((current, None))
        }
      }) 
    )
  }   
  
  /* Get a UserDBSeq and insert the Crew Models.
   * After that the UserDB.read function can parse the 
   *
   */

  private def toUser(users: Seq[(UserDB, ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[AddressDB])]): Future[Seq[User]] = {
    Future.sequence( // open Future.sequence
      //map users to current, so we can use the Seq entrys
      users.map(current => {
        //get the Crews for the Roles-Crew relation. current._7 is the SupporterCrewDB entry
        getCrew(Seq((current._7)))
          // the result is Seq[(Option[SupporterCrewDB], Option[Crew])] 
          .map(_.map(tuple =>
          // build the User Seq
          (current._1, current._2, current._3, current._4, current._5, current._6, tuple._1, tuple._2, current._8)
      ))
    })).map(_.foldLeft[Seq[(UserDB, ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[Crew], Option[AddressDB])]](Seq())
      ((acc, currentList) => acc ++ currentList)).map(UserDB.read( _ ))
  }

  //Wenn er private ist kann ich ihn nutzen. Ansonsten muss ich schauen wo er verwendet wird
  class MariadbUserWS extends UserWS {
    override def find(userId: UUID, queryExtension: JsObject): Future[Option[User]] = ???

    override def list(queryExtension: JsObject, limit : Int, sort: JsObject): Future[List[User]] = ???
  }
  val ws = new MariadbUserWS()
}
