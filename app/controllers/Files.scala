package controllers

import java.util.UUID
import javax.inject.Inject

import scala.concurrent.Future
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.modules.reactivemongo.json._
import models._
import services.UserService
import reactivemongo.api.gridfs.{DefaultFileToSave, FileToSave, GridFS, ReadFile}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by johann on 06.09.17.
  */
class Files @Inject() (
                          userService: UserService,
                          val messagesApi: MessagesApi,
                          val env:Environment[User,CookieAuthenticator],
                          configuration: Configuration,
                          val reactiveMongoApi: ReactiveMongoApi
                        ) extends Silhouette[User,CookieAuthenticator] with MongoController with ReactiveMongoComponents {
  // gridFSBodyParser from `MongoController`
  import MongoController.readFileReads

  type JSONReadFile = ReadFile[JSONSerializationPack.type, JsString]


  private val gridFS = reactiveMongoApi.gridFS
  val fsParser = gridFSBodyParser(gridFS)
  // let's build an index on our gridfs chunks collection if none
  gridFS.ensureIndex().onComplete {
    case index =>
      Logger.info(s"Checked index, result is $index")
  }

  def uploadProfileImage = SecuredAction.async(fsParser) { implicit request =>
    val futureFile: Future[ReadFile[JSONSerializationPack.type, JsValue]] =
      request.body.files.head.ref

    futureFile.map { file => {
      val fileID = UUID.randomUUID()
      gridFS.files.update(
        Json.obj("_id" -> file.id),
        Json.obj("$set" -> Json.obj(
          "user" -> request.identity.id,
          "id" -> fileID,
          "width" -> 400,
          "height" -> 400
        )))
      request.identity.profileFor(request.authenticator.loginInfo)
        .map((profile) => userService.saveImage(profile, LocalProfileImage(fileID)))

      Ok(Json.obj("msg" -> Messages("success.profile.image.upload"), "url" -> routes.Files.get(fileID.toString).url))
//      Redirect(routes.Application.profile).flashing(
//        "success" -> Messages("success.profile.image.upload"))
    }}.fallbackTo(
      Future.successful(
//        Redirect(routes.Application.profile).flashing(
//        "error" -> Messages("error.profile.image.upload"))
        Ok(Json.obj("msg" -> Messages("error.profile.image.upload")))
      )
    )
  }

  def get(id: String) = SecuredAction.async { implicit request =>
    // find the matching attachment, if any, and streams it to the client
    val file = gridFS.find[JsObject, JSONReadFile](Json.obj("id" -> id))

    request.getQueryString("inline") match {
      case Some("true") =>
        serve[JsString, JSONReadFile](gridFS)(file, CONTENT_DISPOSITION_INLINE)

      case _            => serve[JsString, JSONReadFile](gridFS)(file)
    }
  }
}
