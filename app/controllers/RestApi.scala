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
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import daos.{CrewDao, OauthClientDao, UserDao}
import daos.{AccessRightDao, TaskDao}
import models.database.{AccessRight, TaskDB}
import services.{TaskService, UserService, UserTokenService}
import utils.Mailer
import utils.Query.{QueryAST, QueryLexer, QueryParser}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.Stack
import scala.util.parsing.json.JSONArray

//import net.liftweb.json._

class RestApi @Inject() (
  val userDao : UserDao,
  val crewDao: CrewDao,
  val taskDao: TaskDao,
  val accessRightDao: AccessRightDao,
  val oauthClientDao : OauthClientDao,
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
    *  @param A Reads json datatype and request.
    *  @return if the json in the request.body is  successful, return JsSuccess Type, else return BadRequest with json errors
    */
  def validateJson[A: Reads] = BodyParsers.parse.json.validate(_.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e))))

  def profile = SecuredAction.async { implicit request =>
    val json = Json.toJson(request.identity.profileFor(request.authenticator.loginInfo).get)
    val prunedJson = json.transform(
      (__ \ 'loginInfo).json.prune andThen 
      (__ \ 'passordInfo).json.prune andThen 
      (__ \ 'oauth1Info).json.prune)
    prunedJson.fold(
      _ => Future.successful(InternalServerError(Json.obj("error" -> Messages("error.profileError")))),
      js => Future.successful(Ok(js))
    )
  }

  override def onNotAuthenticated(request:RequestHeader) = {
    Some(Future.successful(Unauthorized(Json.obj("error" -> Messages("error.profileUnauth")))))
  }

  def getUsers = ApiAction.async { implicit request => {
    def body(query : JsObject, limit : Int, sort : JsObject) = userDao.ws.list(query, limit, sort).map(users => Ok(
      Json.toJson(users.map(PublicUser(_)))
    ))
    implicit val u: UserDao = userDao
    implicit val config : RequestConfig = UserRequest
    request.query match {
      case Some(query) => query.toExtension.flatMap((ext) =>
        body(ext._1, ext._2.get("limit").getOrElse(20), query.getSortCriteria)
      )
      case None => body(Json.obj(), 20, Json.obj())
    }
  }}

  def getUser(id : UUID) = ApiAction.async { implicit request => {
    def body(query: JsObject) = userDao.ws.find(id, query).map(_ match {
      case Some(user) => Ok(Json.toJson(PublicUser(user)))
      case _ => BadRequest(Json.obj("error" -> Messages("rest.api.canNotFindGivenUser", id)))
    })
    implicit val u: UserDao = userDao
    implicit val config : RequestConfig = UserRequest
    request.query match {
      case Some(query) => query.toExtension.flatMap((ext) => body(ext._1))
      case None => body(Json.obj())
    }

  }}

  case class CreateUserBody(
     email: String,
     firstName : String,
     lastName: String,
     mobilePhone: String,
     placeOfResidence: String,
     birthday: Long,
     sex: String,
     profileImageUrl: Option[String]
  )
  object CreateUserBody{
    implicit val createUserBodyJsonFormat = Json.format[CreateUserBody]
  }

  def createUser() = ApiAction.async(validateJson[CreateUserBody]) { implicit request => {
    val signUpData = request.request.body

    val loginInfo = LoginInfo(CredentialsProvider.ID, signUpData.email)
    userService.retrieve(loginInfo).flatMap{
      case Some(_) =>
        Future(BadRequest(Json.obj("error" -> Messages("error.userExists", signUpData.email))))
      case None =>{
        val profile = Profile(loginInfo, false, signUpData.email, signUpData.firstName, signUpData.lastName, signUpData.mobilePhone, signUpData.placeOfResidence, signUpData.birthday, signUpData.sex, List(new DefaultProfileImage))
        avatarService.retrieveURL(signUpData.email).flatMap(avatarUrl  => {
          userService.save(User(id = UUID.randomUUID(), profiles =
            (signUpData.profileImageUrl, avatarUrl) match {
              case (Some(url), Some(gravatarUrl))  => List(profile.copy(avatar = List(UrlProfileImage(url),GravatarProfileImage(gravatarUrl),new DefaultProfileImage)))
              case (None, Some(gravatarUrl))=> List(profile.copy(avatar = List(GravatarProfileImage(gravatarUrl),new DefaultProfileImage)))
              case (Some(url), None) => List(profile.copy(avatar = List(UrlProfileImage(url), new DefaultProfileImage)))
              case _ => List(profile.copy(avatar = List(new DefaultProfileImage)))
            })).map((user) => Ok(Json.toJson(user)))
        })
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
    val loginInfo : LoginInfo = LoginInfo(CredentialsProvider.ID, userData.email)
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
              case None => Future(NotFound(Messages("error.profileError")))
            }
          }
          case false => Future(BadRequest(Messages("error.identifiersDontMatch")))
        }
        case None => Future(NotFound(Messages("error.noUser")))
      }
    })
  }}

  case class UpdateUserProfileImageBody(
                                         email : String,
                                         url : String
                                       )

  object UpdateUserProfileImageBody {
    implicit val updateUserProfileImageBody = Json.format[UpdateUserProfileImageBody]
  }

  def updateUserProfileImage(id : UUID) = ApiAction.async(validateJson[UpdateUserProfileImageBody]){implicit request =>{
    val userData = request.request.body
    val loginInfo : LoginInfo = LoginInfo(CredentialsProvider.ID, userData.email)
    userDao.find(loginInfo).flatMap(userObj => {
      userObj match {
        case Some(user) => user.id == id match {
          case true => user.profileFor(loginInfo) match {
            case Some(profile) => userService.saveImage(profile, UrlProfileImage(userData.url)).map(u => Ok(Json.toJson(u)))
            case None => Future(NotFound(Messages("error.profileError")))
          }
          case false => Future(BadRequest(Messages("error.identifiersDontMatch")))
        }
        case None => Future(NotFound(Messages("error.noUser")))
      }
    })
  }}

  case class DeleteUserBody(email : String)
  object DeleteUserBody {
    implicit val deleteUserBodyJsonFormat = Json.format[DeleteUserBody]
  }

  def deleteUser(id: UUID) = ApiAction.async(validateJson[DeleteUserBody]){ implicit request =>{
    val loginInfo : LoginInfo = LoginInfo(CredentialsProvider.ID, request.request.body.email)
    userDao.find(loginInfo).flatMap(userObj => {
      userObj match {
        case Some(user) => user.id == id match {
          case true => userDao.delete(id).map(r => Ok(Json.toJson(r)))
          case false => Future(BadRequest(Messages("error.identifiersDontMatch")))
        }
        case None => Future(NotFound(Messages("error.noUser")))
      }
    })
  }}

  def crews = ApiAction.async { implicit request => {
    def body(query: JsObject, limit: Int, sort: JsObject) = crewDao.ws.list(query, limit, sort).map((crews) => Ok(Json.toJson(crews)))

    implicit val c: CrewDao = crewDao
    implicit val config : RequestConfig = CrewRequest
    request.query match {
      case Some(query) => query.toExtension.flatMap((ext) =>
        body(ext._1, ext._2.get("limit").getOrElse(20), query.getSortCriteria)
      )
      case None => body(Json.obj(), 20, Json.obj())
    }
  }}

  //ToDo: ApiAction instead of Action
  def getTasks = Action.async{ implicit  request => {
    taskDao.all().map(tasks => Ok(Json.toJson(tasks)))
  }}

  def getTasksForUser(userId : UUID) = Action.async{ implicit request => {
    taskDao.forUser(userId).map(tasks => Ok(Json.toJson(tasks)))
  }}

  def getAccessRightsForUser(userId : UUID) = Action.async{ implicit  request => {
    userService.accessRights(userId).map(accessRights => Ok(Json.toJson(accessRights)))
  }}

  def getTasksWithAccessRights(id: Long) = Action.async{ implicit request => {
    taskService.getWithAccessRights(id).map(r => Ok(r))
  }}

  def findTask(id: Long) = Action.async{ implicit  request => {
    taskDao.find(id).map(task => Ok(Json.toJson(task)))
  }}

  def createTask() = Action.async(validateJson[TaskDB]) { implicit request => {
    taskDao.create(request.body).map{task => Ok(Json.toJson(task))}
  }}

  def deleteTask(id: Long) = Action.async{ implicit request => {
    taskDao.delete(id).map(count => if (count == 0) NotFound else Ok)
  }}

  //ToDo: Query parameter optional?
  def getAccessRights(query: String, f: String) = Action.async{ implicit  request => {
    //Use the lexer to validate query syntax and extract tokens
    val tokens = QueryLexer(query)
    if(tokens.isLeft){
      Future(BadRequest(Json.obj("error" -> Messages("rest.api.syntaxError"))))
    }else {
      //Use the parser to validate and extract grammar
      val ast = QueryParser(tokens.right.get)
      if (ast.isLeft) {
        Future(InternalServerError(Json.obj("error" -> Messages("rest.api.syntaxGrammar"))))
      }
      else {


        val query: QueryAST = ast.right.get
        //validate if the desired functions exists in the query
        query match {
          case and : utils.Query.A => and.step1 match {
            case step1: utils.Query.EQ => and.step2 match {
              case step2: utils.Query.EQ => {
                val and: utils.Query.A = query.asInstanceOf[utils.Query.A]

                val step1 = and.step1
                val step2 = and.step2
                val filter: JsObject = Json.parse(f).as[JsObject]
                //check, if there exists an filter value for the steps respectively the functions
                if (QueryAST.validateStep(step1, filter) && QueryAST.validateStep(step2, filter)) {
                  //Get the filter values
                  //ToDo This should be generic
                  val userId = UUID.fromString(filter.\("user").\("id").as[String])
                  val service: String = filter.\("accessRight").\("service").as[String]

                  accessRightDao.forUserAndService(userId, service).map(accessRights => Ok(Json.toJson(accessRights)))
                } else {
                  Future(BadRequest(Json.obj("error" -> Messages("rest.api.missingFilterValue"))))
                }
              }
              case _ => Future(NotImplemented(Json.obj("error" -> Messages("rest.api.queryFunctionsNotImplementedYet"))))
            }
            case _ => Future(NotImplemented(Json.obj("error" -> Messages("rest.api.queryFunctionsNotImplementedYet"))))
          }
          case _ => Future(NotImplemented(Json.obj("error" -> Messages("rest.api.queryFunctionsNotImplementedYet"))))
        }
      }
    }
  }}

  def findAccessRight(id: Long) = Action.async{ implicit  request => {
    accessRightDao.find(id).map(accessRights => Ok(Json.toJson(accessRights)))
  }}

  def createAccessRight() = Action.async(validateJson[AccessRight]) {implicit request => {
    accessRightDao.create(request.body).map{ac => Ok(Json.toJson(ac))}
  }}

  def deleteAccessRight(id: Long) = Action.async{ implicit request => {
    accessRightDao.delete(id).map(count => if (count == 0) NotFound else Ok)
  }}


}
