package controllers

import java.util.UUID
import javax.inject.Inject

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import models.{AccessRight, OauthClient, PublicUser, Task, User}
import User._
import api.ApiAction
import api.query.{CrewRequest, RequestConfig, UserRequest}
import daos.{CrewDao, OauthClientDao, UserDao}
import daos.{AccessRightDao, TaskDao}
import services.{TaskService, UserService}

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

  def users = ApiAction.async { implicit request => {
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

  def user(id : String) = ApiAction.async { implicit request => {
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

  def getAccessRights(query: String, f: String) = Action.async{ implicit  request => {
    //ToDo: Query parameter optional?
    val filter : JsObject = Json.parse(f).as[JsObject]
    val userId = UUID.fromString(filter.\("user").\("id").as[String])
    val service: String = filter.\("accessRight").\("service").as[String]

    accessRightDao.forUserAndService(userId, service).map(accessRights => Ok(Json.toJson(accessRights)))
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
