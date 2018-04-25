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
import models.{Organization}
import models.database.OrganizationDB
import utils.Nats

class OrganizationService @Inject() (organizationDAO:OrganizationDAO, nats: Nats) {
  def retrieve():Future[Option[Organization]] = ???
  def save(organization: Organization) = organizationDAO.create(OrganizationDB(organization))
  
}

