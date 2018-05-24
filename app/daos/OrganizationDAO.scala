package daos

import java.util.UUID

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import play.api.Play
import play.api.Logger
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
  def update(organization: Organization): Future[Option[Organization]]
  def addProfile(profileEmail: String, organizationId: UUID): Future[Option[Organization]]
  def checkProfileOranization(profileEmail: String, organizationId: UUID): Future[Boolean]
  def withProfile(id: Long): Future[Option[Organization]]
  def withProfile(id: UUID): Future[Option[Organization]]
  def delete(id: UUID): Future[Int]
  def deleteProfile(id: UUID, email: String): Future[Int]
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
        r.headOption.map(_.toOrganization(None, None)))

  def find(id: UUID): Future[Option[Organization]] = dbConfig.db.run(organizations.filter(o => o.publicId === id).result).map( r =>
        r.headOption.map(_.toOrganization(None, None)))

  def find(name: String): Future[Option[Organization]] = dbConfig.db.run(organizations.filter(o => o.name === name).result).map(r => 
      r.headOption.map(_.toOrganization(None, None)))

 
  def update(organization: Organization): Future[Option[Organization]] = {
    var organization_id = 0L
    dbConfig.db.run((organizations.filter(r => r.publicId === organization.publicId || r.name === organization.name)).result)
      .flatMap(o =>{
        organization_id = o.head.id
        dbConfig.db.run((organizations.filter(_.id === o.head.id).update(OrganizationDB(o.head.id, organization))))
      }).flatMap((id) => find(id))
  }
  
  def addProfile(profileEmail: String, organizationId: UUID): Future[Option[Organization]] = {
    val organization_id = Await.result(dbConfig.db.run(organizations.filter(o => o.publicId === organizationId).result).map( o =>{
      o.head.id}), 10 second)
    val profile_id = Await.result(dbConfig.db.run((profiles.filter(p => p.email === profileEmail)).result).map( p =>{
      p.head.id }), 10 second )
    val dummy = Await.result(dbConfig.db.run((profileOrganizations.map(po => (po.profileId, po.organizationId)) += ((profile_id, organization_id)))), 10 second)
      find(organization_id) 
    }
  
  def checkProfileOranization(profileEmail: String, organizationId:UUID): Future[Boolean] = {
    val organization_id = Await.result(dbConfig.db.run(organizations.filter(o => o.publicId === organizationId).result).map( o =>{
      o.head.id}), 10 second)
    val profile_id = Await.result(dbConfig.db.run((profiles.filter(p => p.email === profileEmail)).result).map( p =>{
      p.head.id }), 10 second )
    dbConfig.db.run((profileOrganizations.filter(uo => uo.organizationId === organization_id && uo.profileId === profile_id)).exists.result)
  }

  def withProfile(id: Long): Future[Option[Organization]] = {
    val action = for {
        o <- organizations.filter(o => o.id === id) 
        op <- profileOrganizations.filter(uo => uo.organizationId === o.id) 
        p <- profiles.filter(p => p.id === op.profileId)
        
    } yield (o, p)
      dbConfig.db.run(action.result).map(p => {OrganizationConverter.buildOrganizationFromResult(p)
      })
    }

  def withProfile(id: UUID): Future[Option[Organization]] = {
      val action = for {
        o <- organizations.filter(o => o.publicId === id) 
        op <- profileOrganizations.filter(uo => uo.organizationId === o.id) 
        p <- profiles.filter(p => p.id === op.profileId)
        
    } yield (o, p)
      dbConfig.db.run(action.result).map(p => {OrganizationConverter.buildOrganizationFromResult(p)
      })
    
  }

  def delete(id: UUID): Future[Int] = {
    val organization_id = Await.result(dbConfig.db.run(organizations.filter(o => o.publicId === id).result).map( o =>{
      o.head.id}), 10 second)
    dbConfig.db.run(organizations.filter(o => o.id === organization_id).delete)
  }

  def deleteProfile(id: UUID, profileEmail: String): Future[Int] = {
    val organization_id = Await.result(dbConfig.db.run(organizations.filter(o => o.publicId === id).result).map( o =>{
      o.head.id}), 10 second)
    val profile_id = Await.result(dbConfig.db.run((profiles.filter(p => p.email === profileEmail)).result).map( p =>{
      p.head.id }), 10 second )
    dbConfig.db.run((profileOrganizations.filter(op => op.profileId === profile_id && op.organizationId === organization_id).delete))
  }



  private def findOrganizationDBModel(id: UUID): Future[OrganizationDB] = {
    dbConfig.db.run(organizations.filter(_.publicId === id).result).map(o => o.head)
  }
}
