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
import play.modules.reactivemongo.json.collection.JSONCollection
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

  val files = reactiveMongoApi.db.collection[JSONCollection]("fs.files")

  def uploadProfileImage = SecuredAction.async(fsParser) { implicit request =>
    val futureFile: Future[ReadFile[JSONSerializationPack.type, JsValue]] =
      request.body.files.head.ref

    futureFile.map { file => {
      val fileID = file.id.validate[UUID].get
      gridFS.files.update(
        Json.obj("_id" -> file.id),
        Json.obj("$set" -> Json.obj(
          "user" -> request.identity.id,
          "width" -> 400,
          "height" -> 400
        )))

      // Delete all old profile images. Can be removed for issue #82!
      request.identity.profileFor(request.authenticator.loginInfo)
        .map { (profile) =>
          profile.avatar.filter(_ match {
            case pI : LocalProfileImage => true
            case _ => false
          }).map(_.asInstanceOf[LocalProfileImage]).map(
            (pi) => {
              gridFS.remove(Json toJson pi.uuid).map(_.hasErrors)
            })
        }
      // Associate new local profile image to the user
      request.identity.profileFor(request.authenticator.loginInfo)
        .map((profile) => userService.saveImage(profile, LocalProfileImage(fileID)))

      Ok(Json.obj("msg" -> Messages("success.profile.image.upload"), "url" -> routes.Files.get(fileID.toString).url))
    }}.fallbackTo(
      Future.successful(
        Ok(Json.obj("msg" -> Messages("error.profile.image.upload")))
      )
    )
  }

  def get(id: String) = SecuredAction.async { implicit request =>
    // find the matching attachment, if any, and streams it to the client
    val file = gridFS.find[JsObject, JSONReadFile](Json.obj("_id" -> id))

    request.getQueryString("inline") match {
      case Some("true") =>
        serve[JsString, JSONReadFile](gridFS)(file, CONTENT_DISPOSITION_INLINE)

      case _            => serve[JsString, JSONReadFile](gridFS)(file)
    }
  }
}
