package controllers

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
import services.{TaskService, UserService, UserTokenService}
import utils.Mailer

import scala.concurrent.ExecutionContext.Implicits.global
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

  def getUser(id : String) = ApiAction.async { implicit request => {
    def body(query: JsObject) = userDao.ws.find(UUID.fromString(id), query).map(_ match {
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
     password: String
  )
  object CreateUserBody{
    implicit val createUserBodyJsonFormat = Json.format[CreateUserBody]
  }

  def createUser() = ApiAction.async(validateJson[CreateUserBody]) { implicit request => {
    val signUpData = request.request.body

    val loginInfo = LoginInfo(CredentialsProvider.ID, signUpData.email)
    userService.retrieve(loginInfo).map {
      case Some(_) =>
        BadRequest(Json.obj("error" -> Messages("error.userExists", signUpData.email)))
      case None =>
      val profile = Profile(loginInfo, signUpData.email, signUpData.firstName, signUpData.lastName, signUpData.mobilePhone, signUpData.placeOfResidence, signUpData.birthday, signUpData.sex, List(new DefaultProfileImage))
      for {
          avatarUrl <- avatarService.retrieveURL(signUpData.email)
          user <- userService.save(User(id = UUID.randomUUID(), profiles =
            avatarUrl match {
              case Some(url) => List(profile.copy(avatar = List(GravatarProfileImage(url),new DefaultProfileImage)))
              case _ => List(profile.copy(avatar = List(new DefaultProfileImage)))
            }))
          _ <- authInfoRepository.add(loginInfo, passwordHasher.hash(signUpData.password))
          token <- userTokenService.save(UserToken.create(user.id, signUpData.email, true))
        } yield {
          mailer.welcome(profile, link = "https://" + request.request.host +  routes.Auth.signUp(token.id.toString).toString())
        }
        Ok(Json.toJson(profile))
    }
  }}

  case class UpdateUserBody(
                             email: String,
                             firstName : Option[String],
                             lastName: Option[String],
                             mobilePhone: Option[String],
                             placeOfResidence: Option[String],
                             birthday: Option[Long],
                             sex: Option[String],
                             password: Option[String]
                           )
  object UpdateUserBody {
    def apply(tuple: (String, Option[String], Option[String], Option[String], Option[String], Option[Long], Option[String], Option[String])) : UpdateUserBody =
      UpdateUserBody(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7, tuple._8)

    implicit val updateUserBodyWrites : OWrites[UpdateUserBody] = (
      (JsPath \ "email").write[String] and
        (JsPath \ "lastName").writeNullable[String] and
        (JsPath \ "fullName").writeNullable[String] and
        (JsPath \ "mobilePhone").writeNullable[String] and
        (JsPath \ "placeOfResidence").writeNullable[String] and
        (JsPath \ "birthday").writeNullable[Long] and
        (JsPath \ "sex").writeNullable[String] and
        (JsPath \ "password").writeNullable[String]
      )(unlift(UpdateUserBody.unapply))
    implicit val updateUserBodyReads : Reads[UpdateUserBody] = (
      (JsPath \ "email").read[String] and
        (JsPath \ "firstName").readNullable[String] and
        (JsPath \ "lastName").readNullable[String] and
        (JsPath \ "mobilePhone").readNullable[String] and
        (JsPath \ "placeOfResidence").readNullable[String] and
        (JsPath \ "birthday").readNullable[Long] and
        (JsPath \ "sex").readNullable[String] and
        (JsPath \ "passwod").readNullable[String]
      ).tupled.map(UpdateUserBody( _ ))
  }

  def updateUser() = ApiAction.async(validateJson[UpdateUserBody]){ implicit request =>{
    val userData = request.request.body
    val loginInfo : LoginInfo = LoginInfo(CredentialsProvider.ID, userData.email)
    val supporter : Supporter = Supporter(userData.firstName, userData.lastName, userData.mobilePhone, userData.placeOfResidence, userData.birthday, userData.sex)
    userDao.find(loginInfo).flatMap(userObj => {
      userObj match {
        case Some(user) => user.profileFor(loginInfo) match {
          case Some(profile) => {profile.supporter.copy(
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
          case None => Future(NotFound(Messages("Profile not found")))
        }
        case None => Future(NotFound(Messages("User not found")))
      }
    })
  }}



  case class DeleteUserBody(id : UUID)
  object DeleteUserBody {
    implicit val deleteUserBodyJsonFormat = Json.format[DeleteUserBody]
  }

  def deleteUser() = ApiAction.async(validateJson[DeleteUserBody]){ implicit request =>{
    val userId : UUID = request.request.body.id
    println(userId)
    userDao.delete(userId).map(r => Ok(Json.toJson(r)))
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

  def createTask() = Action.async(validateJson[Task]) {implicit request => {
    taskDao.create(request.body).map{task => Ok(Json.toJson(task))}
  }}

  def deleteTask(id: Long) = Action.async{ implicit request => {
    taskDao.delete(id).map(count => if (count == 0) NotFound else Ok)
  }}


  def getAccessRights = Action.async{ implicit  request => {
      accessRightDao.all().map(accessRights => Ok(Json.toJson(accessRights)))
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
