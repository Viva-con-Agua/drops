package controllers

import java.net.URL
import java.util.UUID
import javax.inject.Inject

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import com.mohiva.play.silhouette.api.{Environment, LoginInfo, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.api.services.AvatarService
import play.api._
import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, OWrites, Reads}
import play.api.mvc._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import models._
import User._
import api.ApiAction
import api.query.{CrewRequest, RequestConfig, UserRequest}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{PasswordHasher, PasswordInfo}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.impl.util.BCryptPasswordHasher
import daos._
import models.dbviews.{Crews, Users}
import services.{TaskService, UserService, UserTokenService}
import utils.Mailer
import utils.Query._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.Stack
import scala.util.parsing.json.JSONArray
import services.{CrewService}


//import net.liftweb.json._

class RestApi @Inject() (
  val userDao : MariadbUserDao,
  val crewDao: MariadbCrewDao,
  val taskDao: TaskDao,
  val crewService: CrewService,
  val accessRightDao: AccessRightDao,
  val oauthClientDao : OauthClientDao,
  val pool1UserDao : Pool1UserDao,
  val userService : UserService,
  val taskService : TaskService,
  val ApiAction : ApiAction,
  val messagesApi: MessagesApi,
  val avatarService: AvatarService,
  val authInfoRepository: AuthInfoRepository,
  val passwordHasher: PasswordHasher,
  val userTokenService: UserTokenService,
  val mailer: Mailer,
  val env:Environment[User,CookieAuthenticator]) extends Silhouette[User,CookieAuthenticator] {

  /** checks whether a json is validate or not
    *
    *  @param B Reads json datatype and request.
    *  @return if the json in the request.body is  successful, return JsSuccess Type, else return BadRequest with json errors
    */

  def validateJson[B: Reads] = BodyParsers.parse.json.validate(_.validate[B].asEither.left.map(e => BadRequest(JsError.toJson(e))))

  
  def profile = SecuredAction.async { implicit request =>
    val json = Json.toJson(request.identity.profileFor(request.authenticator.loginInfo).get)
    val prunedJson = json.transform(
      (__ \ 'loginInfo).json.prune andThen 
      (__ \ 'passordInfo).json.prune andThen 
      (__ \ 'oauth1Info).json.prune)
    prunedJson.fold(
      _ => Future.successful(responseError(play.api.mvc.Results.InternalServerError, "InternalProfileError", Messages("error.profileError"))),
      js => Future.successful(Ok(js))
    )
  }

  override def onNotAuthenticated(request:RequestHeader) = {
    Some(Future.successful(responseError(play.api.mvc.Results.Unauthorized, "UserNotAuthorized", Messages("error.profileUnauth"))))
  }

  def getUsers = ApiAction.async(validateJson[rest.QueryBody]) { implicit request => {
    implicit val m = messagesApi
    rest.QueryBody.asUsersQuery(request.body) match {
      case Left(e : QueryParserError) => Future.successful(BadRequest(Json.obj("error" -> e.getMessage)))
      case Left(e : rest.QueryBody.NoValuesGiven) => Future.successful(BadRequest(Json.obj("error" -> e.getMessage)))
      case Left(e) => Future.successful(InternalServerError(Json.obj("error" -> e.getMessage)))
      case Right(converter) => try {
        userDao.list_with_statement(converter.toStatement).map((users) =>
          Ok(Json.toJson(users.map(PublicUser( _ ))))
        )
      } catch {
        case e: java.sql.SQLException => {
          Future.successful(InternalServerError(Json.obj("error" -> e.getMessage)))
        }
        case e: Exception => {
          Future.successful(InternalServerError(Json.obj("error" -> e.getMessage)))
        }
      }
    }
  }}

  def getUser(id : UUID) = ApiAction.async { implicit request => {

    userDao.find(id).map(_ match {
      case Some(user) => Ok(Json.toJson(PublicUser(user)))
      case _ => responseError(play.api.mvc.Results.NotFound, "UserNotFound", Messages("rest.api.canNotFindGivenUser", id))
    })
  }}

  protected def responseError(code: play.api.mvc.Results.Status, typ: String, msg: String) : Result =
    responseError(code, typ, JsString(msg))

  protected def responseError(code: play.api.mvc.Results.Status, typ: String, msg: JsValue) : Result =
    code(Json.obj(
      "status" -> "error",
      "code" -> code.header.status,
      "type" -> typ,
      "msg" -> msg
    )).as("application/json")

  case class CreateUserBody(
     email: String,
     firstName : String,
     lastName: String,
     mobilePhone: String,
     placeOfResidence: String,
     birthday: Long,
     sex: String,
     password: Option[String] //Password is optional, perhaps its necessary to set the pw on the first login
  )
  object CreateUserBody{
    implicit val createUserBodyJsonFormat = Json.format[CreateUserBody]
  }

  def createUser() = ApiAction.async(validateJson[CreateUserBody]) { implicit request => {
    val signUpData = request.request.body

    val loginInfo = LoginInfo(CredentialsProvider.ID, signUpData.email.toLowerCase)
    userService.retrieve(loginInfo).flatMap{
      case Some(user) =>
        Future.successful(Created(Json.toJson(PublicUser(user))))
      case None =>{
        var passwordInfo : Option[PasswordInfo] = None
        if(signUpData.password.isDefined)
          passwordInfo = Option(passwordHasher.hash(signUpData.password.get))
        val profile = Profile(loginInfo, false, signUpData.email, signUpData.firstName, signUpData.lastName, signUpData.mobilePhone, signUpData.placeOfResidence, signUpData.birthday, signUpData.sex, passwordInfo)
        userService.save(User(id = UUID.randomUUID(), List(profile),
          updated = System.currentTimeMillis(), created = System.currentTimeMillis())).map((user) => Ok(Json.toJson(user)))
        }
    }
  }}

  case class UpdateUserBody(
                             email: String,
                             firstName : Option[String],
                             lastName: Option[String],
                             mobilePhone: Option[String],
                             placeOfResidence: Option[String],
                             birthday: Option[Long],
                             sex: Option[String]
                           )
  object UpdateUserBody {
    implicit val updateUserBodyJsonFormat = Json.format[UpdateUserBody]
  }

  def updateUser(id : UUID) = ApiAction.async(validateJson[UpdateUserBody]){ implicit request =>{
    val userData = request.request.body
    val loginInfo : LoginInfo = LoginInfo(CredentialsProvider.ID, userData.email.toLowerCase)
    userDao.find(loginInfo).flatMap(userObj => {
      userObj match {
        case Some(user) => user.id == id match{
          case true => {
            user.profileFor(loginInfo) match {
              case Some(profile) => {
                val supporter : Supporter = profile.supporter.copy(
                  firstName = userData.firstName,
                  lastName = userData.lastName,
                  birthday = userData.birthday,
                  mobilePhone = userData.mobilePhone,
                  placeOfResidence = userData.placeOfResidence,
                  sex = userData.sex
                )
                val updatedProfile = profile.copy(supporter = supporter, email = Some(userData.email))
                userService.update(userObj.get.updateProfile(updatedProfile)).map((u) => Ok(Json.toJson(u)))
              }
              case None => Future.successful(responseError(play.api.mvc.Results.NotFound, "ProfileNotFound", Messages("error.profileError")))
            }
          }
          case false => Future.successful(responseError(play.api.mvc.Results.BadRequest, "UserIDMismatch", Messages("error.identifiersDontMatch")))
        }
        case None => Future.successful(responseError(play.api.mvc.Results.NotFound, "UserNotFound", Messages("error.noUser")))
      }
    })
  }}

  case class UpdateUserProfileImageBody(
                                         email : String,
                                         url : String
                                       )

  /*object UpdateUserProfileImageBody {
    implicit val updateUserProfileImageBody = Json.format[UpdateUserProfileImageBody]
  }

  def updateUserProfileImage(id : UUID) = ApiAction.async(validateJson[UpdateUserProfileImageBody]){implicit request =>{
    val userData = request.request.body
    val loginInfo : LoginInfo = LoginInfo(CredentialsProvider.ID, userData.email.toLowerCase)
    userDao.find(loginInfo).flatMap(userObj => {
      userObj match {
        case Some(user) => user.id == id match {
          case true => user.profileFor(loginInfo) match {
            case Some(profile) => userService.saveImage(profile, UrlProfileImage(userData.url)).map(u => Ok(Json.toJson(u)))
            case None => Future.successful(responseError(play.api.mvc.Results.NotFound, "ProfileNotFound", Messages("error.profileError")))
          }
          case false => Future.successful(responseError(play.api.mvc.Results.BadRequest, "UserIDMismatch", Messages("error.identifiersDontMatch")))
        }
        case None => Future.successful(responseError(play.api.mvc.Results.NotFound, "UserNotFound", Messages("error.noUser")))
      }
    })
  }}*/

  case class DeleteUserBody(email : String)
  object DeleteUserBody {
    implicit val deleteUserBodyJsonFormat = Json.format[DeleteUserBody]
  }

  def deleteUser(id: UUID) = ApiAction.async(validateJson[DeleteUserBody]){ implicit request =>{
    val loginInfo : LoginInfo = LoginInfo(CredentialsProvider.ID, request.request.body.email.toLowerCase)
    userDao.find(loginInfo).flatMap(userObj => {
      userObj match {
        case Some(user) => user.id == id match {
          case true => userDao.delete(id).map(r => Ok(Json.toJson(r)))
          case false => Future.successful(responseError(play.api.mvc.Results.BadRequest, "UserIDMismatch", Messages("error.identifiersDontMatch")))
        }
        case None => Future.successful(responseError(play.api.mvc.Results.NotFound, "UserNotFound", Messages("error.noUser")))
      }
    })
  }}
  
  def createCrews = ApiAction.async(validateJson[CrewStub]) { implicit request => {
    val crewData = request.request.body
    crewService.get(crewData.name).flatMap{
      case Some(crew) => Future.successful(Created(Json.toJson(crew)))
      case _ => crewService.save(crewData).flatMap{
        case (crew) => Future.successful(Ok(Json.toJson(crew)))
       // case _ => Future.successful(BadRequest("ERROR"))
      }
    }
  }}

  def getCrews = ApiAction.async(validateJson[rest.QueryBody]){ implicit request => {
    implicit val m = messagesApi
    rest.QueryBody.asCrewsQuery(request.body) match {
      case Left(e : QueryParserError) => Future.successful(BadRequest(Json.obj("error" -> e.getMessage)))
      case Left(e : rest.QueryBody.NoValuesGiven) => Future.successful(BadRequest(Json.obj("error" -> e.getMessage)))
      case Left(e) => Future.successful(InternalServerError(Json.obj("error" -> e.getMessage)))
      case Right(converter) => try {
        crewDao.list_with_statement(converter.toStatement).map((crews) =>
          Ok(Json.toJson(crews))
        )
      } catch {
        case e: java.sql.SQLException => {
          Future.successful(InternalServerError(Json.obj("error" -> e.getMessage)))
        }
        case e: Exception => {
          Future.successful(InternalServerError(Json.obj("error" -> e.getMessage)))
        }
      }
    }
  }}

  def assignUserToCrew(uuidUser: UUID, uuidCrew: UUID, pillar: Option[String]) = ApiAction.async { implicit request =>
    def assignRole(user: User, onlyAssignmentInsert : Boolean = false) : Future[Result] = {
      pillar match {
        case Some(p) => Pillar(p) match {
          case Unknown => Future.successful(NotFound(Messages("rest.assignUserToCrew.assignRole.unknownPillar")))
          case pillarInstance : Pillar => crewService.get(uuidCrew).flatMap(_ match {
            case Some(crew) => userService.assignCrewRole(crew, VolunteerManager(crew, pillarInstance), user).map(_ match {
              case Left(i) if i > 0 => Ok(Messages("profile.assign.crew.success"))
              case Left(i) => NotModified
              case Right(msg) => NotFound(msg)
            })
            case None => Future.successful(NotFound(Messages("rest.assignUserToCrew.assignRole.unknownCrew")))
          })
        }
       case None => Future.successful(onlyAssignmentInsert match {
          case true => Ok(Messages("profile.assign.crew.success"))
          case false => BadRequest(Messages("rest.assignUserToCrew.assignRole.pillarNotGiven"))
        })
      }
    }
    userService.find(uuidUser).flatMap{
      case Some(user) => userService.assignOnlyOne(uuidCrew, user).flatMap(_ match {
        case Left(i) if i > 0 => assignRole(user, true)
        case Left(i) => assignRole(user, false)
        case Right(msg) => Future.successful(NotFound(msg))
      })
      case _ => Future.successful(NotFound(Messages("rest.api.canNotFindGivenUser", uuidUser)))
    }
  }

  //ToDo: ApiAction instead of Action
  def getTasks = ApiAction.async{ implicit  request => {
    taskDao.all().map(tasks => Ok(Json.toJson(tasks)))
  }}

  def getTasksForUser(userId : UUID) = ApiAction.async{ implicit request => {
    taskDao.forUser(userId).map(tasks => Ok(Json.toJson(tasks)))
  }}

  def getAccessRightsForUser(userId : UUID) = ApiAction.async{ implicit  request => {
    userService.accessRights(userId).map(accessRights => Ok(Json.toJson(accessRights)))
  }}

  def getTasksWithAccessRights(id: Long) = ApiAction.async{ implicit request => {
    taskService.getWithAccessRights(id).map(r => Ok(r))
  }}

  def findTask(id: Long) = ApiAction.async{ implicit  request => {
    taskDao.find(id).map(task => Ok(Json.toJson(task)))
  }}

  def createTask() = ApiAction.async(validateJson[Task]) { implicit request => {
    taskDao.create(request.body).map{task => Ok(Json.toJson(task))}
  }}

  def deleteTask(id: Long) = ApiAction.async{ implicit request => {
    taskDao.delete(id).map(count => if (count == 0) NotFound else Ok)
  }}

  //ToDo: Query parameter optional?
  def getAccessRights() = ApiAction.async{ implicit  request => {
    accessRightDao.all().map(tasks => Ok(Json.toJson(tasks)))
  }}

  def findAccessRight(id: Long) = ApiAction.async{ implicit  request => {
    accessRightDao.find(id).map(accessRights => Ok(Json.toJson(accessRights)))
  }}

  def createAccessRight() = ApiAction.async(validateJson[AccessRight]) {implicit request => {
    accessRightDao.create(request.body).map{ac => Ok(Json.toJson(ac))}
  }}

  def deleteAccessRight(id: Long) = ApiAction.async{ implicit request => {
    accessRightDao.delete(id).map(count => if (count == 0) NotFound else Ok)
  }}

  def createPool1User() = ApiAction.async(validateJson[Pool1User]) { request =>
    pool1UserDao.save(request.body).map{user => Ok(Json.toJson(user))}
  }
}
