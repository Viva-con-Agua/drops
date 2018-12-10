package controllers.webapp

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, File}

import play.api.mvc._

import scala.concurrent.Future
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import javax.imageio.ImageIO
import javax.inject.Inject
import models.{RESTImageRequest, UploadedImage, User}
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.Files.TemporaryFile
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import services.UserService
import play.filters.csrf._

import scala.concurrent.ExecutionContext.Implicits.global


class Avatar @Inject() (
                         userService: UserService,
                         val messagesApi: MessagesApi,
                         val env: Environment[User, CookieAuthenticator]
                       ) extends Silhouette[User, CookieAuthenticator] {
  implicit val mApi = messagesApi
  override val logger = Logger(Action.getClass)
  var files : Map[String, UploadedImage] = Map()
  var thumbnailFiles : Map[String, List[UploadedImage]] = Map()

  def validateJson[B: Reads] = BodyParsers.parse.json.validate(_.validate[B].asEither.left.map(e => BadRequest(JsError.toJson(e))))

  private def thumbId(id: String, width: Int, height: Int) : String = id + "_" + width + "x" + height

  def getCSRF = CSRFAddToken { SecuredAction.async { request =>
    Future.successful(
      CSRF.getToken(request) match {
        case Some(token) => WebAppResult.Ok(request, "avatar.upload.csrf", Nil, "Avatar.Upload.CSRF", Json.obj("token" -> token.value)).getResult
        case _ => WebAppResult.Generic(request, play.api.mvc.Results.InternalServerError, "avatar.error.noToken", Nil, "Avatar.Upload.NoCSRFToken", Map[String, String]()).getResult
      }
    )
  }}

  def upload = SecuredAction.async(parse.multipartFormData) { request =>
    val image = request.body.file("image")
    val response = image match {
      case Some(img) => {
        val newImages = List(UploadedImage(img.ref.file, Some(img.filename), img.contentType))
        this.files = this.files ++ newImages.map(uploaded => (img.filename -> uploaded))
        WebAppResult.Ok(request, "avatar.upload.success", Nil, "Avatar.Upload.Success",
          Json.toJson(newImages.map((img) => img.getRESTResponse(
            controllers.webapp.routes.Avatar.get(img.name).url
          )))
        ).getResult
      }
      case None => WebAppResult.Ok(request, "avatar.upload.failure", Nil, "Avatar.Upload.Failure", Json.obj()).getResult
    }

    Future.successful(response)
  }

  def thumbnails(id : String) = SecuredAction.async(validateJson[List[RESTImageRequest]]) { request =>
    val response = request.body.map(_.toUploadedImage)
    this.thumbnailFiles = this.thumbnailFiles ++ Map(id -> response)
    Future.successful(
      WebAppResult.Ok(request, "avatar.upload.success", Nil, "Avatar.Thumbnail.Success",
        Json.toJson(response.map((thumb) => thumb.getRESTResponse(
          controllers.webapp.routes.Avatar.getThumb(thumb.name, thumb.width, thumb.height).url
        )))
      ).getResult
    )
  }

  def get(id: String) = SecuredAction.async { request =>
    Future.successful(this.files.get(id) match {
      case Some(img) => {
        val temp = TemporaryFile(img.getName, img.format).file
        ImageIO.write(img.bufferedImage, img.format, temp)
        Ok.sendFile(temp).as(img.getContentType)
      }
      case _ => WebAppResult.NotFound(request, "avatar.get.notFound", Nil, "Avatar.Get.NotFound", Map(
        "id" -> id
      )).getResult
    })
  }

  def getThumb(id: String, width: Int, height: Int) = SecuredAction.async { request =>
    def finder(id: String, width: Int, height: Int, ui: UploadedImage) = ui ~ (id, width, height)
    Future.successful(this.thumbnailFiles.get(id).flatMap(_.find((ui: UploadedImage) => finder(id, width, height, ui))) match {
      case Some(thumb) => {
        val temp = TemporaryFile(thumb.getName, thumb.format).file
        ImageIO.write(thumb.bufferedImage, thumb.format, temp)
        Ok.sendFile(temp).as(thumb.getContentType)
      }
      case _ => WebAppResult.NotFound(request, "avatar.getThumb.notFound", Nil, "Avatar.GetThumb.NotFound", Map(
        "id" -> id,
        "width" -> width.toString,
        "height" -> height.toString
      )).getResult
    })
  }
}
