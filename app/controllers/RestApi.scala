package controllers

import java.util.UUID
import javax.inject.Inject

import scala.concurrent.Future
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api._
import play.api.libs.json._

import play.api.mvc._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import models.{OauthClient, PublicUser, Task, User}
import User._
import api.ApiAction
import api.query.{CrewRequest, RequestConfig, UserRequest}
import daos.{CrewDao, OauthClientDao, UserDao}
import daos.TaskDao
import api.query.TaskRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.parsing.json.JSONArray

//import net.liftweb.json._

class RestApi @Inject() (
  val userDao : UserDao,
  val crewDao: CrewDao,
  val taskDao: TaskDao,
  val oauthClientDao : OauthClientDao,
  val ApiAction : ApiAction,
  val messagesApi: MessagesApi,
  val env:Environment[User,CookieAuthenticator]) extends Silhouette[User,CookieAuthenticator] {

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

  def tasks = Action{
    val t = taskDao.getAll()

    Ok(t)
  }
  def taskCreate = Action{ request: Request[AnyContent] => {
    val error:String = """{ "error": "request body parameter title is required", "code": "400"}"""

    val body: Option[JsValue] = request.body.asJson

      if(body == None){
        BadRequest(Json.parse(error))
      }else if((body.get \"title").asOpt[String] == None) {
        BadRequest(Json.parse(error))
      }else{
          val jsonBody: JsValue = body.get
          val newTaskObj : Task = jsonBody.as[Task]
          val t = taskDao.create(newTaskObj)
          Ok(t)
      }
  }}

  def taskFind(id: Int) = Action{
    val t:JsValue = taskDao.find(id)

    //Check, if one task was found
    if(t.as[JsArray].value.size == 0){
      val error:String = """{ "error": "task not found", "code": "404"}"""
      NotFound(Json.parse(error))
    }else {
      Ok(t)
    }
  }
}
