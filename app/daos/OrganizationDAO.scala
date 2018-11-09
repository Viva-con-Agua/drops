package daos

import java.util.UUID

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import play.api.Play
import play.api.Logger
import models.{Organization, Profile, BankAccount}
import models.database.{OrganizationDB, ProfileDB, ProfileOrganizationDB, BankAccountDB}
import models.converter.{OrganizationConverter}
import daos.schema.{OrganizationTableDef, ProfileTableDef, ProfileOrganizationTableDef, BankAccountTableDef }

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
  def addProfile(profileEmail: String, organizationId: UUID, role: String): Future[Option[Organization]]
  def addProfile(profileEmail: String, organizationName: String, role: String): Future[Option[Organization]]
  def checkProfileOrganization(profileEmail: String, organizationId: UUID): Future[Boolean]
  def checkProfileOrganization(profileEmail: String, organizationName: String): Future[Boolean]
  def withProfile(id: Long): Future[Option[Organization]]
  def withProfile(id: UUID): Future[Option[Organization]]
  def withProfileByRole(id: UUID, role: String): Future[Option[Organization]]
  def delete(id: UUID): Future[Int]
  def deleteProfile(id: UUID, email: String): Future[Int]
  def withBankAccounts(id: Long): Future[Option[Organization]]
  def withBankAccounts(publicId: UUID): Future[Option[Organization]]
  def withBankAccounts(name: String): Future[Option[Organization]]
  def addBankAccount(bankaccount: BankAccount, publicId: UUID): Future[Option[Organization]]
  def addBankAccount(bankaccount: BankAccount, name: String): Future[Option[Organization]]
}

class MariadbOrganizationDAO extends OrganizationDAO {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  val organizations = TableQuery[OrganizationTableDef]
  val profileOrganizations = TableQuery[ProfileOrganizationTableDef]
  val profiles = TableQuery[ProfileTableDef]
  val bankaccounts = TableQuery[BankAccountTableDef]

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
  
  def addProfile(profileEmail: String, organizationId: UUID, role: String): Future[Option[Organization]] = {
    dbConfig.db.run(organizations.filter(o => o.publicId === organizationId).result).flatMap( o =>{
      dbConfig.db.run((profiles.filter(p => p.email === profileEmail)).result).flatMap( p =>{
        dbConfig.db.run((profileOrganizations.map(po => (po.profileId, po.organizationId, po.role)) += ((p.head.id, o.head.id, role)))).flatMap(op => withProfile(o.head.id))
    })})
    }

  def addProfile(profileEmail: String, organizationName: String, role: String): Future[Option[Organization]] = {
    dbConfig.db.run(organizations.filter(o => o.name === organizationName).result).flatMap( o =>{
      dbConfig.db.run((profiles.filter(p => p.email === profileEmail)).result).flatMap( p =>{
        dbConfig.db.run((profileOrganizations.map(po => (po.profileId, po.organizationId, po.role)) += ((p.head.id, o.head.id, role)))).flatMap(op => withProfile(o.head.id))
    })})
  }
  
  def checkProfileOrganization(profileEmail: String, organizationId:UUID): Future[Boolean] = {
     dbConfig.db.run(organizations.filter(o => o.publicId === organizationId).result).flatMap( o =>{
      dbConfig.db.run((profiles.filter(p => p.email === profileEmail)).result).flatMap( p =>{
        dbConfig.db.run((profileOrganizations.filter(uo => uo.organizationId === o.head.id && uo.profileId === p.head.id)).exists.result)
      })
    })
  }
  def checkProfileOrganization(profileEmail: String, organizationName:String): Future[Boolean] = {
    dbConfig.db.run(organizations.filter(o => o.name === organizationName).result).flatMap( o =>{
      dbConfig.db.run((profiles.filter(p => p.email === profileEmail)).result).flatMap( p =>{
        dbConfig.db.run((profileOrganizations.filter(uo => uo.organizationId === o.head.id && uo.profileId === p.head.id)).exists.result)
      })
    })
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

  def withProfileByRole(id: UUID, role: String): Future[Option[Organization]] = {
    val action = for {
      o <- organizations.filter(o => o.publicId === id)
      op <- profileOrganizations.filter(po => po.organizationId === o.id && po.role == role)
      p <- profiles.filter(p => p.id === op.profileId) 
    } yield(o, p)
    dbConfig.db.run(action.result).map(p => {OrganizationConverter.buildOrganizationFromResult(p)})
  }


  def delete(id: UUID): Future[Int] = {
    dbConfig.db.run(organizations.filter(o => o.publicId === id).result).flatMap( og =>{
      dbConfig.db.run(organizations.filter(od => od.id === og.head.id).delete)
    })
  }

  def deleteProfile(id: UUID, profileEmail: String): Future[Int] = {
    dbConfig.db.run(organizations.filter(o => o.publicId === id).result).flatMap( o =>{
      dbConfig.db.run((profiles.filter(p => p.email === profileEmail)).result).flatMap( p =>{
        dbConfig.db.run((profileOrganizations.filter(op => op.profileId === p.head.id && op.organizationId === o.head.id).delete))
      })
    })
  }

  /* Bankaccount 
   *
   */
  
  def withBankAccounts(id: Long): Future[Option[Organization]] = {
    val action = for {
      o <- organizations.filter(o => o.id === id)
      b <- bankaccounts.filter(b => b.organization_id === id)

    }yield (o, b)
    dbConfig.db.run(action.result).map(ob => {OrganizationConverter.buildOrganizationBankAccountFromResult(ob)})
  }
  
  def withBankAccounts(publicId: UUID): Future[Option[Organization]] = {
    val action = for {
      o <- organizations.filter(o => o.publicId === publicId)
      b <- bankaccounts.filter(b => b.organization_id === o.id)

    }yield (o, b)
    dbConfig.db.run(action.result).map(ob => {OrganizationConverter.buildOrganizationBankAccountFromResult(ob)})
  }
  def withBankAccounts(name: String): Future[Option[Organization]] = {
    val action = for {
      o <- organizations.filter(o => o.name === name)
      b <- bankaccounts.filter(b => b.organization_id === o.id)

    }yield (o, b)
    dbConfig.db.run(action.result).map(ob => {OrganizationConverter.buildOrganizationBankAccountFromResult(ob)})
  }


  def addBankAccount(bankaccount: BankAccount, organizationId: UUID): Future[Option[Organization]] = {
    dbConfig.db.run(organizations.filter(o => o.publicId === organizationId).result).flatMap( o => {
      dbConfig.db.run((bankaccounts returning bankaccounts.map(_.id) +=  BankAccountDB(0, bankaccount.bankName, bankaccount.number, bankaccount.blz, bankaccount.iban, bankaccount.bic, o.head.id)))
    }).flatMap(ob => withBankAccounts(organizationId))
  }
   def addBankAccount(bankaccount: BankAccount, organizationName: String): Future[Option[Organization]] = {
    dbConfig.db.run(organizations.filter(o => o.name === organizationName).result).flatMap( o => {
      dbConfig.db.run((bankaccounts returning bankaccounts.map(_.id) +=  BankAccountDB(0, bankaccount.bankName, bankaccount.number, bankaccount.blz, bankaccount.iban, bankaccount.bic, o.head.id)))
    }).flatMap(ob => withBankAccounts(organizationName))
  }
 
  /*  
    val action = for {
      o <- organizations.filter(o => o.publicId === organizationId)
      b <- (bankaccounts returning bankaccounts.map(_.id)) += BankaccountDB(0, bankaccount.bankName, bankaccount.number, bankaccount.blz, bankaccount.iban, bankaccount.bic, o.id.result)
    }yield (o)
    dbConfig.db.run(action.result).flatMap((id) => find(id))
 }*/


  private def findOrganizationDBModel(id: UUID): Future[OrganizationDB] = {
    dbConfig.db.run(organizations.filter(_.publicId === id).result).map(o => o.head)
  }
}
