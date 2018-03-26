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
import services.{UserService, DispenserService}
import daos.{CrewDao, OauthClientDao, TaskDao}
import play.api.libs.json.{JsPath, JsValue, Json, Reads}
import play.api.libs.ws._
import utils.authorization.{Pool1Restriction, WithAlternativeRoles, WithRole}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global

class Application @Inject() (
  oauth2ClientDao: OauthClientDao,
  userService: UserService,
  crewDao: CrewDao,
  taskDao: TaskDao,
  ws: WSClient,
  passwordHasher: PasswordHasher,
  val messagesApi: MessagesApi,
  val env:Environment[User,CookieAuthenticator],
  configuration: Configuration,
  dispenserService: DispenserService,
  socialProviderRegistry: SocialProviderRegistry) extends Silhouette[User,CookieAuthenticator] {

  val pool1Export = configuration.getBoolean("pool1.export").getOrElse(false)
  val pool1Url = configuration.getString("pool1.url").get

  def index = SecuredAction(Pool1Restriction(pool1Export)).async { implicit request =>
    /*val template: Template = dispenserService.buildTemplate(
      NavigationData("GlobalNav", "", None),
      "Drops", views.html.index(request.identity, request.authenticator.loginInfo).toString
      )
    val dispenserResult = Future.successful(Ok(views.html.dispenser(dispenserService.getSimpleTemplate(template))))
    */
    if (!pool1Export) {
      Future.successful(Ok(dispenserService.getTemplate(views.html.index(request.identity, request.authenticator.loginInfo))))
    }else{
      Future.successful(Redirect(pool1Url))
    }
  }

  def profile = SecuredAction(Pool1Restriction(pool1Export)).async { implicit request =>
    crewDao.list.map(l => {
      Ok(dispenserService.getTemplate(views.html.profile(request.identity, request.authenticator.loginInfo, socialProviderRegistry, UserForms.userForm, CrewForms.geoForm, l.toSet, PillarForms.define)))
    })
  }

  def updateBase = SecuredAction(Pool1Restriction(pool1Export)).async { implicit request =>
    UserForms.userForm.bindFromRequest.fold(
      bogusForm => crewDao.list.map(l => BadRequest(views.html.profile(request.identity, request.authenticator.loginInfo, socialProviderRegistry, bogusForm, CrewForms.geoForm, l.toSet, PillarForms.define))),
      userData => request.identity.profileFor(request.authenticator.loginInfo) match {
        case Some(profile) => {
          val supporter = profile.supporter.copy(
            firstName = Some(userData.firstName),
            lastName = Some(userData.lastName),
            fullName = Some(s"${userData.firstName} ${userData.lastName}"),
            birthday = Some(userData.birthday.getTime),
            mobilePhone = Some(userData.mobilePhone),
            placeOfResidence = Some(userData.placeOfResidence),
            sex = Some(userData.sex)
          )
          val updatedProfile = profile.copy(supporter = supporter, email = Some(userData.email))
          userService.update(request.identity.updateProfile(updatedProfile)).map((u) => Redirect(routes.Application.profile))
        }
        case _ => Future.successful(Redirect(routes.Application.profile))
      }
    )
  }

  def updateCrew = SecuredAction(Pool1Restriction(pool1Export)).async { implicit request =>
    CrewForms.geoForm.bindFromRequest.fold(
      bogusForm => crewDao.list.map(l => BadRequest(views.html.profile(request.identity, request.authenticator.loginInfo, socialProviderRegistry, UserForms.userForm, bogusForm, l.toSet, PillarForms.define))),
      crewData => {
        request.identity.profileFor(request.authenticator.loginInfo) match {
          case Some(profile) => {
            crewDao.find(crewData.crewName).map( _ match {
              case Some(crew) => {
                val updatedSupporter = profile.supporter.copy(crew = Some(crew))
                val updatedProfile = profile.copy(supporter = updatedSupporter)
                userService.update(request.identity.updateProfile(updatedProfile))
                Redirect("/profile")
              }
              case None => Redirect("/profile")
            })

          }
          case None =>  Future.successful(InternalServerError(Messages("crew.update.noProfileForLogin")))
        }
      }
    )
  }

  def updatePillar = SecuredAction(Pool1Restriction(pool1Export)).async { implicit request =>
    PillarForms.define.bindFromRequest.fold(
      bogusForm => crewDao.list.map(l => BadRequest(views.html.profile(request.identity, request.authenticator.loginInfo, socialProviderRegistry, UserForms.userForm, CrewForms.geoForm, l.toSet, bogusForm))),
      pillarData => request.identity.profileFor(request.authenticator.loginInfo) match {
        case Some(profile) => {
          val pillars = pillarData.toMap.foldLeft[Set[Pillar]](Set())((pillars, data) => data._2 match {
            case true => pillars + Pillar(data._1)
            case false => pillars
          })
          val supporter = profile.supporter.copy(pillars = pillars)
          val updatedProfile = profile.copy(supporter = supporter)
          userService.update(request.identity.updateProfile(updatedProfile)).map((u) => Redirect("/"))
        }
        case _ => Future.successful(Redirect("/"))
      }
    )
  }

  def task = SecuredAction(Pool1Restriction(pool1Export)) { implicit request =>
    val resultingTasks: Future[Seq[Task]] = taskDao.all()
      Ok(dispenserService.getTemplate(views.html.task(request.identity, request.authenticator.loginInfo, resultingTasks)))
  }

  def initCrews = SecuredAction(WithRole(RoleAdmin) && Pool1Restriction(pool1Export)).async { request =>
    configuration.getConfigList("crews").map(_.toList.map(c =>
      crewDao.find(c.getString("name").get).map(_ match {
        case Some(crew) => crew
        case _ => crewDao.save(
          Crew(UUID.randomUUID(), c.getString("name").get, c.getString("country").get, c.getStringList("cities").get.toSet)
        )
      })
    ))
    Future.successful(Redirect("/"))
  }

  def fixCrewsID = SecuredAction(WithRole(RoleAdmin) && Pool1Restriction(pool1Export)).async { request =>
    val crews = crewDao.listOfStubs.flatMap(l => Future.sequence(l.map(oldCrew => crewDao.update(oldCrew.toCrew))))
    val users = crews.flatMap(l => userService.listOfStubs.flatMap(ul => Future.sequence(ul.map(user => {
      val profiles = user.profiles.map(profile => {
        val newCrewSupporter = profile.supporter.crew.flatMap((crew) => l.find(_.name == crew.name))
        profile.toProfile(newCrewSupporter)
      })
      userService.update(user.toUser(profiles))
    }))))
    val res = for {
      crewsList <- crews
      usersList <- users
    } yield (crewsList, usersList)
    res.map(pair => Ok(Json.arr(Json.toJson(pair._1), Json.toJson(pair._2))))
  }

  def initUsers(number: Int, specialRoleUsers : Int = 1) = SecuredAction(WithRole(RoleAdmin) && Pool1Restriction(pool1Export)).async { request => {
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
    crewDao.ws.list(Json.obj(),150,Json.obj()).flatMap((crews) =>
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

  def registration = SecuredAction((WithRole(RoleAdmin) || WithRole(RoleEmployee)) && Pool1Restriction(pool1Export)) { implicit request =>
    val template: Template = dispenserService.buildTemplate(
        NavigationData("GlobalNav", "", None),
        "Drops", views.html.oauth2.register(request.identity, request.authenticator.loginInfo, socialProviderRegistry, OAuth2ClientForms.register).toString
      )
      Ok(views.html.dispenser(dispenserService.getSimpleTemplate(template)))
  }

  def registerOAuth2Client = SecuredAction((WithRole(RoleAdmin) || WithRole(RoleEmployee)) && Pool1Restriction(pool1Export)).async { implicit request =>
    OAuth2ClientForms.register.bindFromRequest.fold(
      bogusForm => Future.successful(BadRequest(views.html.oauth2.register(request.identity, request.authenticator.loginInfo, socialProviderRegistry, bogusForm))),
      registerData => {
        oauth2ClientDao.save(registerData.toClient)
        Future.successful(Redirect("/"))
      }
    )
  }
}

object OAuth2ClientForms {
  case class OAuth2ClientRegister(id:String, secret: String, redirectUri: Option[String], grantTypes: Set[String]) {
    def toClient = OauthClient(id, secret, redirectUri, grantTypes)
  }
  def register = Form(mapping(
    "id" -> nonEmptyText,
    "secret" -> nonEmptyText,
    "redirectUri" -> optional(text),
    "grantTypes" -> nonEmptyText
  )
  ((id, secret, redirectUri, grantTypes) => OAuth2ClientRegister(id, secret, redirectUri, grantTypes.split(",").toSet))
  ((rawData) => Some((rawData.id, rawData.secret, rawData.redirectUri, rawData.grantTypes.mkString(",")))))
}
