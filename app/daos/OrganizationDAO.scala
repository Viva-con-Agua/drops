package daos

import java.util.UUID

import scala.concurrent.Future
import play.api.Play

import models.{Organization, Profile}
import models.database.{OrganizationDB, ProfileDB, ProfileOrganizationDB}
import models.converter.{OrganizationConverter}
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
  def update(organization: Organization): Future[Organization]
  def withProfile(id: Long): Future[Option[Organization]]
  def withProfile(id: UUID): Future[Option[Organization]]
  def delete(id: UUID): Future[Option[Organization]]
  def deleteProfile(id: UUID, email: String): Future[Option[Organization]]
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

 
  def update(organization: Organization): Future[Organization] = {
    var organization_id = 0L
    dbConfig.db.run((organizations.filter(r => r.publicId === organization.publicId || r.name === organization.name)).result)
      .flatMap(o =>{
        organization_id = o.head.id
        dbConfig.db.run((organizations.filter(_.id === o.head.id).update(OrganizationDB(o.head.id, organization))))
      }).flatMap(_ => find(organization_id)).map(o => o.get)
  }
  
  def addProfile(profileEmail: String, organizationId: UUID): Future[Option[Organization]] = {
    var profile_id = 0L
    var organization_id = 0L
    dbConfig.db.run(organizations.filter(o => o.publicId === organizationId).result).map( o =>{
      organization_id = o.head.id})
    dbConfig.db.run((profiles.filter(p => p.email === profileEmail)).result).map( p =>{
      profile_id = p.head.id })
    dbConfig.db.run((profileOrganizations returning profileOrganizations.map(po => (po.profileId, po.organizationId)) += ((profile_id, organization_id))))
      .flatMap((id) => withProfile(id._1)) 
    }
  
  def withProfile(id: Long): Future[Option[Organization]] = {
    val action = for {
        o <- organizations.filter(o => o.id === id) 
        (p, _) <- profiles join profileOrganizations.filter(uo => uo.organizationId === o.id) on (_.id === _.profileId)
        
    } yield(o, p)
      dbConfig.db.run(action.result).map(OrganizationConverter.buildOrganizationFromResult(_))
  }

  def withProfile(id: UUID): Future[Option[Organization]] = {
      val action = for {
        o <- organizations.filter(o => o.publicId === id) 
        (p, _) <- profiles join profileOrganizations.filter(uo => uo.organizationId === o.id) on (_.id === _.profileId)
        
    } yield(o, p)
      dbConfig.db.run(action.result).map(OrganizationConverter.buildOrganizationFromResult(_))
    
  }
  def delete(id: UUID): Future[Option[Organization]] = ???

  def deleteProfile(id: UUID, email: String): Future[Option[Organization]] = ???

  private def findOrganizationDBModel(id: UUID): Future[OrganizationDB] = {
    dbConfig.db.run(organizations.filter(_.publicId === id).result).map(o => o.head)
  }
}
