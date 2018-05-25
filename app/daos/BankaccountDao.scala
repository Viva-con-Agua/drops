package daos

import java.util.UUID

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import play.api.Play
import play.api.Logger
import models.{Organization, Profile, Bankaccount}
import models.database.{OrganizationDB, ProfileDB, ProfileOrganizationDB}
import models.converter.{OrganizationConverter}
import daos.schema.{OrganizationTableDef, ProfileTableDef, ProfileOrganizationTableDef, BankaccountTableDef }

import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import slick.driver.MySQLDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

/** BankaccountDao
 *  
 *  
 *
 */

/*trait BankaccountDao {
  def all(oID: Long): Future[Seq[Bankaccount]]
  def create(bankaccount: Bankaccount, oID: Long): Future[Option[Bankaccount]]
  def find(id: Long): Future[Option[Bankaccount]]
  def find(oID: Long): Future[Option[Bankaccount]]
}*/
