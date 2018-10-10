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
import daos.{AccessRightDao, CrewDao, UserDao}
import models._
import controllers.rest.QueryBody
import utils.Nats

class CrewService @Inject() (crewDao: CrewDao, nats: Nats) {
  def save(crewStub: CrewStub) = crewDao.save(crewStub.toCrew)
  def update(crew: Crew) = crewDao.update(crew)
  def delete(crew: Crew) = ???
  def get(id: UUID) = crewDao.find(id)
  def get(name: String) = crewDao.find(name)
  def list(queryBody: QueryBody) = crewDao.list
}
