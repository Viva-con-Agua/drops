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
  val profileOrganizations = TableQuery[ProfileOrganizationTableDef]
  val profiles = TableQuery[ProfileTableDef]

  def all(): Future[Seq[Organization]] = ???
 

  def create(organization: OrganizationDB): Future[Option[Organization]] = {
    dbConfig.db.run((organizations returning organizations.map(_.id)) += organization).flatMap((id) => find(id))
  }

  def find(id: Long): Future[Option[Organization]] = dbConfig.db.run(organizations.filter(o => o.id === id).result).map( r => 
        r.headOption.map(_.toOrganization(None)))

  def find(id: UUID): Future[Option[Organization]] = dbConfig.db.run(organizations.filter(o => o.publicId === id).result).map( r =>
        r.headOption.map(_.toOrganization(None)))

  def find(name: String): Future[Option[Organization]] = dbConfig.db.run(organizations.filter(o => o.name === name).result).map(r => 
      r.headOption.map(_.toOrganization(None)))

  
  def withProfile(id: UUID): Future[Seq[ProfileDB]] = ???
    /*dbConfig.db.run(organizations.filter(o => o.publicId === id).result).flatMap(organization => {
      val action = for {
        (o, _) <- (profiles join profileOrganization.filter(uo => uo.organizationId === organization.head.id) on (_.id === _.profileId))
    } yield(o)
      dbConfig.db.run(action.result)
    })
  }*/
  
  /*def saveProfile(profileId : UUID, organizationId : UUID) : Future[Seq[Organization]] = {
    dbConfig.db.run(organizatons.filter(o => o.pulicId == id).result).
      val action = for {
        (o,)
      profileOrganizations)
  }*/

  private def findOrganizationDBModel(id: UUID): Future[OrganizationDB] = {
    dbConfig.db.run(organizations.filter(_.publicId === id).result).map(o => o.head)
  }
}
