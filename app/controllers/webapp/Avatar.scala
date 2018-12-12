package controllers.webapp

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, File}
import java.util.UUID

import play.api.mvc._

import scala.concurrent.Future
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import javax.imageio.ImageIO
import javax.inject.Inject
import models.{RESTImageResponse, RESTImageThumbnailResponse, UploadedImage, User}
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.libs.Files.TemporaryFile
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import services.{AvatarService, UserService}
import play.filters.csrf._

import scala.concurrent.ExecutionContext.Implicits.global


class Avatar @Inject() (
                         userService: UserService,
                         avatarService: AvatarService,
                         val messagesApi: MessagesApi,
                         val env: Environment[User, CookieAuthenticator]
                       ) extends Silhouette[User, CookieAuthenticator] {
  implicit val mApi = messagesApi
  override val logger = Logger(Action.getClass)

  def validateJson[B: Reads] = BodyParsers.parse.json.validate(_.validate[B].asEither.left.map(e => BadRequest(JsError.toJson(e))))

  private def thumbId(id: String, width: Int, height: Int) : String = id + "_" + width + "x" + height

  private def getEmail[T](request: SecuredRequest[T]) : String = request.identity.profiles.head.email.get

  def getCSRF = CSRFAddToken { SecuredAction.async { request =>
    Future.successful(
      CSRF.getToken(request) match {
        case Some(token) => WebAppResult.Ok(request, "avatar.upload.csrf", Nil, "Avatar.Upload.CSRF", Json.obj("token" -> token.value)).getResult
        case _ => WebAppResult.Generic(request, play.api.mvc.Results.InternalServerError, "avatar.error.noToken", Nil, "Avatar.Upload.NoCSRFToken", Map[String, String]()).getResult
      }
    )
  }}

  def getAll = SecuredAction.async { request =>
    avatarService.getAll.map((list) => WebAppResult.Ok(request, "avatar.getAll.success", Nil, "Avatar.GetAll.Success",
      Json.toJson(list.map((uploadedImage) => RESTImageResponse(uploadedImage)))
    ).getResult)
  }

  def upload = SecuredAction.async(parse.multipartFormData) { request =>
    request.body.file("image") match {
      case Some(img) => avatarService.add(img.ref.file, img.filename, img.contentType, this.getEmail(request)).map(_ match {
        case Some(uploadedImage) => WebAppResult.Ok(request, "avatar.upload.success", Nil, "Avatar.Upload.Success",
          Json.toJson(RESTImageResponse(uploadedImage))
        ).getResult
        case _ => WebAppResult.Generic(request, play.api.mvc.Results.InternalServerError, "avatar.upload.internalServerError",
          Nil, "Avatar.Upload.InternalServerError", Map[String, String]()).getResult
      })
      case _ => Future.successful(
        WebAppResult.Bogus(request, "avatar.upload.noImageInRequest", Nil, "Avatar.Upload.NoImageInRequest", Json.obj()).getResult
      )
    }
  }

  def thumbnails(id : String) = SecuredAction.async(parse.multipartFormData) { request =>
    val uploadedImages = request.body.files.map((img) => UploadedImage(img.ref.file, Some(img.filename), img.contentType))
    val uuid = UUID.fromString(id)
    avatarService.replaceThumbs(uuid, uploadedImages.toList, this.getEmail(request)).map(_ match {
      case Left( _ ) => WebAppResult.NotFound(request, "avatar.upload.thumbnail.failure", Nil, "Avatar.Thumbnail.Failure", Map()).getResult
      case Right(thumbs) => WebAppResult.Ok(request, "avatar.upload.thumbnail.success", Nil, "Avatar.Thumbnail.Success",
        Json.toJson(thumbs.map((thumb) => RESTImageThumbnailResponse(thumb, uuid)))
      ).getResult
    })
  }

  def get(id: String) = SecuredAction.async { request =>
    val uuid = UUID.fromString(id)
    avatarService.get(uuid).map(_ match {
      case Some(img) => {
        val temp = TemporaryFile(img.getName, img.format).file
        ImageIO.write(img.bufferedImage, img.format, temp)
        Ok.sendFile(temp).as(img.getContentType)
      }
      case _ => WebAppResult.NotFound(request, "avatar.get.notFound", Nil, "Avatar.Get.NotFound", Map(
        "id" -> uuid.toString
      )).getResult
    })
  }

  def getThumb(id: String, width: Int, height: Int) = SecuredAction.async { request =>
    val uuid = UUID.fromString(id)
    avatarService.getThumb(uuid, width, height).map(_ match {
      case Some(thumb) => {
        val temp = TemporaryFile(thumb.getName, thumb.format).file
        ImageIO.write(thumb.bufferedImage, thumb.format, temp)
        Ok.sendFile(temp).as(thumb.getContentType)
      }
      case _ => WebAppResult.NotFound(request, "avatar.getThumb.notFound", Nil, "Avatar.GetThumb.NotFound", Map(
        "id" -> uuid.toString,
        "width" -> width.toString,
        "height" -> height.toString
      )).getResult
    })
  }

  def remove(id: String) = SecuredAction.async { request =>
    val uuid = UUID.fromString(id)
    avatarService.remove(uuid).map(i =>
      if(i >= 1) {
        WebAppResult.Ok(request, "avatar.remove.success", Nil, "Avatar.Remove.Success",
          Json.obj("id" -> uuid.toString)
        ).getResult
      } else {
        WebAppResult.NotFound(request, "avatar.remove.notFound", Nil, "Avatar.Remove.NotFound",
          Map("id" -> uuid.toString)
        ).getResult
      }
    )
  }
}
