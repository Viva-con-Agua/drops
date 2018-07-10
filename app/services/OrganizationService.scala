package services

import java.util.UUID
import javax.inject._

import scala.concurrent.Future
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json.collection.JSONCollection
import daos.{AccessRightDao, TaskDao, OrganizationDAO}
import models.AccessRight
import models.{Organization, BankAccount}
import models.database.OrganizationDB
import utils.Nats

class OrganizationService @Inject() (organizationDAO:OrganizationDAO, nats: Nats) {
  def retrieve():Future[Option[Organization]] = ???
  def save(organization: Organization) = organizationDAO.create(OrganizationDB(organization))
  def find(publicId: UUID) = organizationDAO.find(publicId)
  def find(name: String) = organizationDAO.find(name)
  def update(organization: Organization): Future[Option[Organization]] = organizationDAO.update(organization)
  def addProfile(email: String, id: UUID, role: String) = organizationDAO.addProfile(email, id, role)
  def addProfile(email: String, name: String, role: String) = organizationDAO.addProfile(email, name, role)
  def checkProfileOrganization(email: String, id: UUID): Future[Boolean] = organizationDAO.checkProfileOrganization(email, id)
  def checkProfileOrganization(email: String, name: String): Future[Boolean] = organizationDAO.checkProfileOrganization(email, name)
  def withProfile(id: UUID) = organizationDAO.withProfile(id)
  def withProfileByRole(id: UUID, role: String) = organizationDAO.withProfileByRole(id, role)
  def delete(id: UUID) = organizationDAO.delete(id)
  def deleteProfile(id: UUID, email: String) = organizationDAO.deleteProfile(id, email)
  def addBankAccount(bankaccount: BankAccount, organizationId: UUID) = organizationDAO.addBankAccount(bankaccount, organizationId)
  def addBankAccount(bankaccount: BankAccount, organizationName: String) = organizationDAO.addBankAccount(bankaccount, organizationName)
  def withBankAccounts(publicId: UUID) = organizationDAO.withBankAccounts(publicId)
  def withBankAccounts(name: String) = organizationDAO.withBankAccounts(name)
}

