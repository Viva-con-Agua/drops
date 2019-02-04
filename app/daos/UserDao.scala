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


  implicit val getUserResult = GetResult(r => UserDB(r.nextLong, UUID.fromString(r.nextString), r.nextString, r.nextLong, r.nextLong))
  implicit val getProfileResult = GetResult(r => ProfileDB(r.nextLong, r.nextBoolean, r.nextString, r.nextLong))
  implicit val getLoginInfoResult = GetResult(r => LoginInfoDB(r.nextLong, r.nextString, r.nextString, r.nextLong))
  implicit val getPasswordInfoResult = GetResult(r => Some(PasswordInfoDB(r.nextLong, r.nextString, r.nextString, r.nextLong)))
  implicit val getSupporterInfoResult = GetResult(r => SupporterDB(r.nextLong, r.nextStringOption, r.nextStringOption, r.nextStringOption, r.nextStringOption, r.nextStringOption, r.nextLongOption, r.nextStringOption, r.nextLong))
  implicit val getSupporterCrewInfoResult = GetResult(r => Some(SupporterCrewDB(r.nextLong, r.nextLong, r.nextLong, r.nextStringOption, r.nextStringOption, r.nextLong, r.nextLong)))
  implicit val getOauth1InfoResult = GetResult(r => Some(OAuth1InfoDB(r.nextLong, r.nextString, r.nextString, r.nextLong)))
  implicit val getAddressInfoResult = GetResult(r => Some(AddressDB(r.nextLong, UUID.fromString(r.nextString), r.nextString, r.nextStringOption, r.nextString, r.nextString, r.nextString, r.nextLong)))
  
  
 /* private def toUserDBSeq(users: 
    Seq[(
      UserDB, 
      ProfileDB, 
      SupporterDB, 
      LoginInfoDB, 
      Option[PasswordInfoDB], 
      Option[OAuth1InfoDB], 
      Option[SupporterCrewDB])]
    ): Future[Seq[User]] = {
    users.groupBy(_._1).mapValues(_.flatMap(_._2)).toSeq
  }*/

  /*private def supporterDBSeq(supporter: Seq[(SupporterDB, Option[SupporterCrewDB], Option[AddressDB])]): Future[Seq[(SupporterDB, Seq[(Option[AddressDB])], Seq[(Option[SupporterCrewDB], Option[Crew])])]] = { 
    supporter.groupBy(_._1).toSeq.map(supporterMapped =>
        (supporterMapped._1)
      )
  }*/
  

  /* build Seq[(Option[Crew], Seq[(Option[SupporterCrewDB])])] from Seq[(Option[SupporterCrewDB])]
   *
   * The Seq is contains all SupporterCrewDB for an Crew in a Seq.
   */
 /* private def getCrewSeq(supporterCrews: Seq[(Option[SupporterCrewDB])]): Future[Seq[(Option[SupporterCrewDB], Option[Crew])]] = {
    Future.sequence(
      supporterCrews.map(entry => {
            //build Seq of pairs via getCrew function
            val crews: Seq[(Option[Crew], Option[SupporterCrewDB])] = Seq()
            entry.foreach( current => 
                crews :+ getCrew(Seq(Option(current))).map(crew => crew)
            )
            //group Seq by Crews and take the head of the new Seq. This is because of the Seq[Seq[]] struct by supporterCrews.map and toSeq 
            crews.groupBy(_._1).toSeq.map(crewMapped =>
              Future.successful((crewMapped._1, crewMapped._2.map(supporterCrewMapped =>
                  (supporterCrewMapped._2)
                  )
                ))
            ).head
            
      })
    )
  }*/
  /* build  Future[Seq[(Option[Crew], Option[SupporterCrewDB])]] from Seq[(Option[SupporterCrewDB])]
   *
   * search Crew for every SupporterCrewDB and pair them. 
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
  
  /*private def toSupporter(seq: Seq[(SupporterDB, Option[SupporterCrewDB], Option[AddressDB])]): Future[Seq[Supporter]] = {
    Future.sequence {
      val supporterDB: SupporterDB = seq.groupBy(_._1).toSeq.map(sortedCurrent => sortedCurrent._1).head
      val address: Set[Address] = seq.groupBy(_._3).toSeq.filter(_._1.isDefined).map(current => current._1.head.toAddress).toSet
      Seq(getCrew(seq.groupBy(_._2).toSeq.map(current => current._1)).map(result => supporterDB.toSupporter(SupporterCrewDB.read(result), address)))
 
      
    }
  }*/
/*
  private def toProfile(profileSeq: Seq[(ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[AddressDB])]) : Future[Seq[Profile]] = {
    Future.sequence {
      //get the SupporterModel
      profileSeq.map(current => toSupporter(Seq((current._2, current._6, current._7))).map(supporter => 
        profileSeq.groupBy(_._1).toSeq.map(profileSeqGroup => profileSeqGroup._1.toProfile(
          profileSeq.groupBy(_._3).toSeq.map(_._1.toLoginInfo).head,
          supporter.head,
          profileSeq.groupBy(_._4).toSeq.map(current => current._1.headOption match {
            case Some(passwordInfo) => Option(passwordInfo.toPasswordInfo)
            case _ => None
          }).head,
          profileSeq.groupBy(_._5).toSeq.map(current => current._1.headOption match {
            case Some(oauthInfo) => Option(oauthInfo.toOAuth1Info)
            case _ => None
          }).head
        )).head
      ))
    }
  }*/

  /*private def toUserA(user:Seq[(UserDB, ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[AddressDB])]): Future[User] = {
    
  }*/
  /*
  private def toUserDBSeq(users: Seq[(UserDB, ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[AddressDB])]): Future[Seq[User]] = {
    Future.sequence(
      //groupBy UserDB Models 
      users.groupBy(_._1).toSeq.map(userMapped =>
          //map to Seq[(UserDB, Seq[(ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB])])]
          (userMapped._1, userMapped._2.map(current =>
            (current._2, current._3, current._4, current._5, current._6, current._7)          
            ).groupBy(_._1).toSeq.map(profileMapped =>
              (profileMapped._1, profileMapped._2.map(current =>
                (current._2, current._3, current._4, current._5. current._6))
              ).groupBy(_._1).toSeq.map(supporterMapped => 
                supporterMapped._1), profileMapped._2.map(current =>
                  (current._3, current._4, current._5)
                ))
              )
            )

        )
  }*/

  private def toUser(users: Seq[(UserDB, ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[AddressDB])]): Future[Seq[User]] = {
    Future.sequence(users.map(current => {
      getCrew(Seq((current._7)))  //get Crew for every SupporterCrewDB
        .map(_.map(tuple =>
        (current._1, current._2, current._3, current._4, current._5, current._6, tuple._1, tuple._2, current._8)
      ))
    })).map(_.foldLeft[
      Seq[(UserDB, ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[Crew], Option[AddressDB])]
      ](Seq())((acc, currentList) => acc ++ currentList)).map(UserDB.read( _ ))
  }

  private def toUserProfiles(profiles: Seq[(ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[AddressDB])]): Future[Seq[Profile]] = {
    addCrews(profiles).map(profiles => ProfileDB.read(profiles))
  }

  private def addCrews(profiles: Seq[(ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[AddressDB])]):
    Future[Seq[(ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[Crew], Option[AddressDB])]] =
    Future.sequence(profiles.map(current =>
      current._6.headOption match {
        case Some(supporterCrew) => crewDao.find(supporterCrew.crewId).map(crew =>
          (current._1, current._2, current._3, current._4, current._5, current._6, crew, current._7)
        )
        case _ => Future.successful((current._1, current._2, current._3, current._4, current._5, current._6, None, current._7))
      }
    ))

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
  


  //Wenn er private ist kann ich ihn nutzen. Ansonsten muss ich schauen wo er verwendet wird
  class MariadbUserWS extends UserWS {
    override def find(userId: UUID, queryExtension: JsObject): Future[Option[User]] = ???

    override def list(queryExtension: JsObject, limit : Int, sort: JsObject): Future[List[User]] = ???
  }
  val ws = new MariadbUserWS()
}
