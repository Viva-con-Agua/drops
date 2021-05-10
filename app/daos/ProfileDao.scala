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

trait ProfileDao {
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
  def removeCrewRole(crewUUID: UUID, crewDBID: Long, role: VolunteerManager, profile: Profile): Future[Either[Int, String]]
  def setActiveFlag(profile: Profile, activeFlag: Option[String]): Future[Boolean]
  def getActiveFlag(profile: Profile): Future[Option[String]]
  def setNVM(profile: Profile, nvmStatus: Option[Long]): Future[Boolean]
  def getNVM(profile: Profile) : Future[Option[Long]]
}

class MariadbProfileDao @Inject()(val crewDao: MariadbCrewDao) extends ProfileDao {
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

  def getProfile(email: String): Future[Option[Profile]] = {
    val action = for{
      ((((((profile, supporter), loginInfo), passwordInfo), oauth1Info), supporterCrew), address) <- ( profiles.filter(p => p.email === email)
        join supporters on (_.id === _.profileId)
        join loginInfos on (_._1.id === _.profileId)
        joinLeft passwordInfos on(_._1._1.id === _.profileId)
        joinLeft oauth1Infos on (_._1._1._1.id === _.profileId)
        joinLeft supporterCrews on (_._1._1._1._2.id === _.supporterId)
        joinLeft addresses on (_._1._1._1._1._2.id === _.supporterId)
      )
    } yield(profile, supporter, loginInfo, passwordInfo, oauth1Info, supporterCrew, address)
    dbConfig.db.run(action.result).flatMap(toUserProfiles( _ )).map(_.headOption)
  }
  
 def profileListByRole(id: UUID, role: String):Future[List[Profile]] = {
    val action = for {
      //o <- organizations.filter(o => o.publicId === id)
      //op <- profileOrganizations.filter(po => po.organizationId === o.id)
      ((((((((organization, profileOrganization), profile), supporter), loginInfo), passwordInfo), oauth1Info), supporterCrew), address) <- ( organizations.filter(o => o.publicId === id)
        join profileOrganizations.filter(op => op.role === role) on (_.id === _.organizationId)
          join profiles on (_._2.profileId === _.id)
          join supporters on (_._2.id === _.profileId)
          join loginInfos on (_._1._2.id === _.profileId)
          joinLeft passwordInfos on(_._1._2.id === _.profileId)
          joinLeft oauth1Infos on (_._1._2.id === _.profileId)
          joinLeft supporterCrews on (_._1._1._2.id === _.supporterId)
          joinLeft addresses on (_._1._1._1._1._2.id === _.supporterId)
        )
    } yield (profile, supporter, loginInfo, passwordInfo, oauth1Info, supporterCrew, address)
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
    updated.supporter.address.isEmpty match {
      case true => Future.successful(None)
      case false => updateAddress(updated)
    }
  ).flatMap(_ =>
    updated.email.map(getProfile( _ )).getOrElse(Future.successful(None))
  )
 }

 def updateAddress(updated: Profile): Future[Option[Profile]] = {
    val action = for { 
    p <- profiles.filter(p => p.email === updated.email)
    s <- supporters.filter(s => s.profileId === p.id)
    a <- addresses.filter(a => a.supporterId === s.id)
    } yield a
   dbConfig.db.run(action.result).flatMap( result => {
    result.isEmpty match {
      case false => dbConfig.db.run(addresses.insertOrUpdate(AddressDB(result.head.id, result.head.publicId, updated.supporter.address.head, result.head.supporterId)))
      case true => insertAddress(updated)
    }
   }).flatMap(_ => updated.email.map(getProfile( _ )).getOrElse(Future.successful(None)))
 }
  
 def insertAddress(profile: Profile) = {
  val action = for {
    p <- profiles.filter(p => p.email === profile.email)
    s <- supporters.filter(s => s.profileId === p.id)
  } yield s
  dbConfig.db.run(action.result).flatMap(result => {
    dbConfig.db.run((addresses returning addresses.map(_.id)) += AddressDB(0, UUID.randomUUID(), profile.supporter.address.head, result.head.id))
  })
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
        case Some(supporter) => Future.sequence((SupporterCrewDB * (supporter.id, crewDB.id, profile.supporter.roles, None, None, None)).map( sc =>
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
            def insert(active: Option[String] = None, nvmDate: Option[Long] = None) = {
              val sc = SupporterCrewDB(supporterDB.id, crewDBID, Some(role), None, active, nvmDate)
              dbConfig.db.run(supporterCrews += sc).map(_ match {
                case i if i > 0 => Left(i)
                case _ => Right("dao.user.error.nothingUpdated")
              })
            }
            dbConfig.db.run(supporterCrews.filter(row =>
              row.supporterId === supporterDB.id && row.crewId === crewDBID
            ).result).flatMap(sc => sc.size match {
              case 0 => insert()
              case i if i > 0 => insert(sc.head.active, sc.head.nvmDate)
              case _ =>  Future.successful(Right("dao.user.error.nothingUpdated"))
            })
          }
          case None => Future.successful(Right("dao.user.error.notFound.supporter"))
        })
      }
      case Some(crew) => Future.successful(Right("dao.user.error.anotherCrewAssigned"))
      case None => Future.successful(Right("dao.user.error.notFound.crew"))
    }
  }

  /**
    * Removes a role to a supporter if and only if, the supporter has already the mentioned crew.
    * @param crewUUID
    * @param crewDBID
    * @param role
    * @param profile
    * @return
    */
  override def removeCrewRole(crewUUID: UUID, crewDBID: Long, role: VolunteerManager, profile: Profile): Future[Either[Int, String]] = {
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
              val updateQ = for { sc <- supporterCrews if sc.supporterId === supporterDB.id && sc.crewId === crewDBID && sc.pillar === role.getPillar.name } yield (sc.role, sc.pillar)
              dbConfig.db.run(updateQ.update((None, None))).map(_ match {
                case i if i > 0 => Left(i)
                case _ => Right("dao.user.error.nothingUpdated")
              })
            }
            dbConfig.db.run(supporterCrews.filter(row =>
              row.supporterId === supporterDB.id && row.crewId === crewDBID && row.pillar === role.getPillar.name
            ).exists.result).flatMap(_ match {
              case true => update
              case false => Future.successful(Right(role.getPillar.name))
            })
          }
          case None => Future.successful(Right("dao.user.error.notFound.supporter"))
        })
      }
      case Some(crew) => Future.successful(Right("dao.user.error.anotherCrewAssigned"))
      case None => Future.successful(Right("dao.user.error.notFound.crew"))
    }
  }

  /**
    * Set the active state of a supporter on his primary crew to the new state
    * @param profile Profile of the supporter
    * @param activeFlag New state of the supporter
    * @return Future[Boolean]
    */
  def setActiveFlag(profile: Profile, activeFlag: Option[String]): Future[Boolean] = {
    // Get the crew of the current profiles supporter
    profile.supporter.crew match {
      case Some(crew) => {
        crewDao.findDBCrewModel(crew.id).flatMap(_ match {
          case Some (crewDB) => {
            // Create query to get the supporters
            val supporterQuery = (for {
              p <- profiles.filter(_.email === profile.email)
              s <- supporters.filter(_.profileId === p.id)
            } yield s)

            // Run query and check if we found a supporter
            dbConfig.db.run(supporterQuery.result).flatMap(_.headOption match {
              case Some(supporterDB) => {
                // Update the state of the of the supporters crew
                val updateQ = for { sc <- supporterCrews if sc.supporterId === supporterDB.id && sc.crewId === crewDB.id } yield (sc.active)
                dbConfig.db.run(updateQ.update(activeFlag)).map(_ match {
                  case x if x > 0 => true
                  case _ => false
                })
              }
              case None => Future.successful(false)
            })
          }
          case _ => Future.successful(false)
        })
      }
      case _ => Future.successful(false)
    }
  }

  /**
    * Get the active state of a supporter from his primary crew
    * @param profile Profile of the supporter
    * @return Future[Option[String]]
    */
  def getActiveFlag(profile: Profile): Future[Option[String]] = {
    // Get the crew of the current profiles supporter
    profile.supporter.crew match {
      case Some(crew) => {
        crewDao.findDBCrewModel(crew.id).flatMap(_ match {
          case Some(crewDB) => {
            // Create query to get the supporters crew and therefore the active state
            val action = for {
              p <- profiles.filter(current => current.email === profile.email) 
              s <- supporters.filter(current => current.profileId === p.id)
              sc <- supporterCrews.filter(current => current.crewId === crewDB.id && current.supporterId === s.id)
            } yield sc.active
            // Run the query and return the active state
            dbConfig.db.run(action.result).map(_.headOption match {
              case Some(activeFlag) => activeFlag
              case _ => None
            })
          }
          case _ => Future.successful(None)
        })
      }
      case _ => Future.successful(None)
    } 
  }

  /**
    * Set the nvm state as an date of a supporter on his primary crew to the new state
    * @param profile Profile of the supporter
    * @param nvmDate New date for nvm state of the supporter
    * @return Future[Boolean]
    */
  def setNVM(profile: Profile, nvmDate: Option[Long]): Future[Boolean] = {
    profile.supporter.crew match {
      // Get the crew of the current profiles supporter
      case Some(crew) => {
        crewDao.findDBCrewModel(crew.id).flatMap(_ match {
          case Some (crewDB) => {
            // Create query to get the supporter
            val supporterQuery = (for {
              p <- profiles.filter(_.email === profile.email)
              s <- supporters.filter(_.profileId === p.id)
            } yield s)
            // Run the query and trigger the update on the result
            dbConfig.db.run(supporterQuery.result).flatMap(_.headOption match {
              case Some(supporterDB) => {
                // Update the state of the of the supporters crew
                val updateQ = for { sc <- supporterCrews if sc.supporterId === supporterDB.id && sc.crewId === crewDB.id } yield (sc.nvmDate)
                dbConfig.db.run(updateQ.update(nvmDate)).map(_ match {
                  case x if x > 0 => true
                  case _ => false
                })
              }
              case None => Future.successful(false)
            })
          }
          case _ => Future.successful(false)
        })
      }
      case _ => Future.successful(false)
    }
  }

  /**
    * Get the nvm date of a supporter from his primary crew
    * @param profile Profile of the supporter
    * @return Future[Option[Long]]
    */
  def getNVM(profile: Profile) : Future[Option[Long]] ={
    profile.supporter.crew match {
      case Some(crew) => {
        // Get the crew of the current profiles supporter
        crewDao.findDBCrewModel(crew.id).flatMap(_ match {
          // Create query to get the supporters crew and therefore the nvm date
          case Some(crewDB) => {
            val action = for {
              p <- profiles.filter(current => current.email === profile.email) 
              s <- supporters.filter(current => current.profileId === p.id)
              sc <- supporterCrews.filter(current => current.crewId === crewDB.id && current.supporterId === s.id)
            } yield sc.nvmDate
            // Run the query and return the nvm date
            dbConfig.db.run(action.result).map(_.headOption match {
              case Some(nvmDate) => nvmDate
              case _ => None
            })
          }
          case _ => Future.successful(None)
        })
      }
      case _ => Future.successful(None)
    }
  }

  private def toUserProfiles(profiles: Seq[(ProfileDB, SupporterDB, LoginInfoDB, Option[PasswordInfoDB], Option[OAuth1InfoDB], Option[SupporterCrewDB], Option[AddressDB])]): Future[Seq[Profile]] = {
    Future.sequence(profiles.map(current => {
      getCrew(Seq((current._6)))  //get Crew for every SupporterCrewDB
        .map(_.map(tuple =>
        (current._1, current._2, current._3, current._4, current._5, tuple._1, tuple._2, current._7))
      ).map(ProfileDB.read(_).head)
    }))
  }
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
}
