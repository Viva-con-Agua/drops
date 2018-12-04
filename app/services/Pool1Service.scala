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
import models.{Pool1User}
import daos.{Pool1UserDao}


class Pool1Service @Inject() (pool1UserDAO: Pool1UserDao){

  def pool1user(email: String) : Future[Option[Pool1User]] = pool1UserDAO.find(email)
  def confirmed(email: String) : Future[Option[Pool1User]] = pool1UserDAO.confirm(email)

}




