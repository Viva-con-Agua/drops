package daos

import java.util.UUID

import scala.concurrent.Future
import play.api.Play

import models.{Organization, Profile}
import models.database.{OrganizationDB, ProfileDB}
//import models.converter.{OrganizationConverter}
import daos.schema.{OrganizationTableDef, ProfileTableDef, ProfileOrganizationTableDef }

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.MySQLDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

trait OrganizationDAO {
  def all(): Future[Seq[Organization]]
  def create(organization: OrganizationDB): Future[Option[Organization]]
  def find(id: Long): Future[Option[Organization]]
  def find(id: UUID): Future[Option[Organization]]
  def find(name: String): Future[Option[Organization]]
  def withProfile(id: UUID): Future[Seq[ProfileDB]]
}

class MariadbOrganizationDAO extends OrganizationDAO {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  val organizations = TableQuery[OrganizationTableDef]
  val profileOrganization = TableQuery[ProfileOrganizationTableDef]
  val profiles = TableQuery[ProfileTableDef]

  def all(): Future[Seq[Organization]] = ???
  
  def find(id: Long): Future[Option[Organization]] = {
    dbConfig.db.run(organizations.filter(o => o.id === id).result).map( r => 
        r.headOption.map(_.toOrganization(None))
      )
  }
  
  def create(organization: OrganizationDB): Future[Option[Organization]] = {
    dbConfig.db.run((organizations returning organizations.map(_.id)) += organization).flatMap((id) => find(id))
  }

  def find(id: UUID): Future[Option[Organization]] = ???
  /*{
    val action = for {
      (profile, organization) <- (organizations.filter(o => o.id == id) join profiles on profileOrganization(uo => uo.organizationId === id) on (_.id === _.profileId))
    } yield(profile, organization)
    dbConfig.db.run(action.result).map(result => { 
      OrganizationConverter.buildOrganizationFromResult(result)
    }
    )
  }*/

  def find(name: String): Future[Option[Organization]] = dbConfig.db.run(organizations.filter(o => o.name === name).result).map(r => 
      r.headOption.map(_.toOrganization(None)))

  def withProfile(id: UUID): Future[Seq[ProfileDB]] = {
    dbConfig.db.run(organizations.filter(o => o.publicId === id).result).flatMap(organization => {
      val action = for {
        (o, _) <- (profiles join profileOrganization.filter(uo => uo.organizationId === organization.head.id) on (_.id === _.profileId))
    } yield(o)
      dbConfig.db.run(action.result)
    })
  }

  private def findOrganizationDBModel(id: UUID): Future[OrganizationDB] = {
    dbConfig.db.run(organizations.filter(_.publicId === id).result).map(o => o.head)
  }
}
