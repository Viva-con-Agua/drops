package controllers.webapp

import java.io.File

import play.api.mvc._

import scala.concurrent.Future
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import javax.inject.Inject
import models.User
import play.api.i18n.MessagesApi
import play.api.libs.json._
import services.UserService
import play.filters.csrf._


class Avatar @Inject() (
                         userService: UserService,
                         val messagesApi: MessagesApi,
                         val env: Environment[User, CookieAuthenticator]
                       ) extends Silhouette[User, CookieAuthenticator] {
  implicit val mApi = messagesApi
  var files : Map[String, File] = Map()

  def getCSRF = CSRFAddToken { SecuredAction.async { request =>
    Future.successful(
      CSRF.getToken(request) match {
        case Some(token) => WebAppResult.Ok(request, "avatar.upload.csrf", Nil, "Avatar.Upload.CSRF", Json.obj("token" -> token.value)).getResult
        case _ => WebAppResult.Generic(request, play.api.mvc.Results.InternalServerError, "avatar.error.noToken", Nil, "Avatar.Upload.NoCSRFToken", Map[String, String]()).getResult
      }
    )
  }}

  def upload = SecuredAction.async(parse.multipartFormData) { request =>
    var res = request.body.file("avatar").map { picture =>
      // only get the last part of the filename
      // otherwise someone can send a path like ../../home/foo/bar.txt to write to other files on the system
      import java.io.File
      val filename = picture.filename
      val contentType = picture.contentType
      val file = picture.ref.file //.moveTo(new File(s"tmp/picture/$filename"))
      this.files += (filename -> file)
      WebAppResult.Ok(request, "avatar.upload.success", Nil, "Avatar.Upload.Success", Json.obj("fileName" -> filename)).getResult
    }.getOrElse {
      WebAppResult.Bogus(request, "avatar.notExist", Nil, "Avatar.Upload.Failure", Json.obj()).getResult
    }
    Future.successful(res)
  }

  def get(id: String) = SecuredAction.async { request =>
    Future.successful(this.files.get(id) match {
      case Some(file) => Ok.sendFile(
        content = file,
        fileName = _ => id
      )
      case _ => WebAppResult.NotFound(request, "avatar.get.notFound", Nil, "Avatar.Get.NotFound", Map(
        "id" -> id
      )).getResult
    })
  }

//  def upload = SecuredAction.async(parse.temporaryFile) { request =>
//    request.body.moveTo(Paths.get("/tmp/picture/uploaded"), replace = true)
//    Ok("File uploaded")
//  }

}
