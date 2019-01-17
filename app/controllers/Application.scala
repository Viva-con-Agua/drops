package controllers

import java.util.UUID

import javax.inject.Inject
import com.mohiva.play.silhouette.api.util.PasswordHasher

import scala.concurrent.Future
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import play.api._
import play.api.mvc._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import models._
import models.dispenser._
import play.api.data.Form
import play.api.data.Forms._
import services.{UserService}
import daos.{CrewDao, OauthClientDao, TaskDao}
import play.api.libs.json.{JsPath, JsValue, Json, Reads}
import play.api.libs.ws._
import utils.authorization.{IsVolunteerManager, Pool1Restriction, WithAlternativeRoles, WithRole}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global

class Application @Inject() (
  userService: UserService,
  crewDao: CrewDao,
  ws: WSClient,
  passwordHasher: PasswordHasher,
  val messagesApi: MessagesApi,
  val env:Environment[User,CookieAuthenticator],
  configuration: Configuration) extends Silhouette[User,CookieAuthenticator] {

  def initCrews = SecuredAction(WithRole(RoleAdmin)).async { request =>
    configuration.getConfigList("crews").map(_.toList.map(c =>
      crewDao.find(c.getString("name").get).map(_ match {
        case Some(crew) => crew
        case _ => crewDao.save(
          Crew(
            UUID.randomUUID(),
            c.getString("name").get,
            c.getStringList("cities").get.toSet.map((name: String) => City(name, c.getString("country").get)))
        )
      })
    ))
    Future.successful(Redirect("/"))
  }
  
  def initUsers(number: Int, specialRoleUsers : Int = 1) = SecuredAction(WithRole(RoleAdmin)).async { request => {
    val wsRequest = ws.url("https://randomuser.me/api/")
      .withHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000)
      .withQueryString("nat" -> "de", "results" -> number.toString, "noinfo" -> "noinfo")

    import play.api.libs.functional.syntax._

    case class UserWsResults(results : List[JsValue])
    implicit val resultsReader : Reads[UserWsResults] = (
      (JsPath \ "results").read[List[JsValue]]
    ).map(UserWsResults(_))

    implicit val pw : PasswordHasher = passwordHasher
    crewDao.list.flatMap((crews) =>
      wsRequest.get().flatMap((response) => {
        val users = (response.json.as[UserWsResults]).results.zipWithIndex.foldLeft[List[DummyUser]](List())(
          (l, userJsonIndex) => l :+ DummyUser(
            userJsonIndex._1,
            (userJsonIndex._2 <= specialRoleUsers)
          )
        ).map((dummy) => {
          // add crew and extract the user
          val crewSupporter = DummyUser.setRandomCrew(dummy, crews.toSet).user
          // save user in database and return a future of the saved user
          userService.save(crewSupporter)
        })
        Future.sequence(users).map((list) => Ok(Json.toJson(list)))
      })
    )
  }}
}
