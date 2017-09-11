package models

import java.net.{URI, URL}
import java.util.UUID

import play.api.libs.json._
import URLHelper._
import controllers.routes
import play.api.libs.functional.syntax._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

/**
  * Created by johann on 06.09.17.
  */
abstract class ProfileImage extends ListableImage {
  def getImage(width: Int, height: Int) : Future[Option[String]]
}

trait ListableImage { image : ProfileImage =>
  def allSizes(implicit ec: ExecutionContext): Future[List[String]] =
    Future.sequence(ProfileImage.sizes.map((pair) => image.getImage(pair("width"), pair("height"))))
      .map(_.foldLeft[List[String]](Nil)((res, opt) => opt match {
        case Some(url) => url :: res
        case _ => res
      }))
}

case class GravatarProfileImage(url: URL) extends ProfileImage {

  val t = ProfileImageType.Gravatar

  override def toString: String = this.url.toURI.toASCIIString

  override def getImage(width: Int, height: Int): Future[Option[String]] = {
    val queryExt = "s=" + width
    val query = url.getQuery match {
      case query: String => query + "&" + queryExt
      case _ => queryExt
    }
    Future.successful(Some(
      new URI(url.toURI.getScheme(), url.toURI.getAuthority(), url.toURI.getPath(), query, url.toURI.getFragment())
        .toURL
        .toExternalForm
    ))
  }
}

case class LocalProfileImage(uuid:UUID) extends ProfileImage {
  val t = ProfileImageType.Local
  /**
    * @todo use width and height parameter!
    *
    * @param width
    * @param height
    * @return
    */
  override def getImage(width: Int, height: Int): Future[Option[String]] =
    Future.successful(Some(routes.Files.get(uuid.toString).url))
}

object GravatarProfileImage {
  def apply(url: String) : GravatarProfileImage = GravatarProfileImage(java.net.URI.create(url).toURL)
}

object ProfileImage {
  /*
    @todo Read this from config file!
   */
  val sizes = List(Map("width" -> 400, "height" -> 400), Map("width" -> 30, "height" -> 30))

  case class JSONProfileImage(typeOf : String, id: Option[UUID], url: Option[URL])

  object JSONProfileImage {
    def apply(pi : ProfileImage) : JSONProfileImage = pi match {
      case gpi : GravatarProfileImage => JSONProfileImage(ProfileImageType.typeToString(gpi.t), None, Some(gpi.url))
      case lpi : LocalProfileImage => JSONProfileImage(ProfileImageType.typeToString(lpi.t), Some(lpi.uuid), None)
    }

    def apply(t: (String, Option[UUID], Option[URL])) : JSONProfileImage = JSONProfileImage(t._1, t._2, t._3)

    implicit val jsonProfileImageWrapperReads: Reads[JSONProfileImage] = (
      (JsPath \ "typeOf").read[String] and
        (JsPath \ "id").readNullable[UUID] and
        (JsPath \ "url").readNullable[URL]
      ).tupled.map(JSONProfileImage( _ ))

    implicit val jsonProfileImageWrapperWrites: OWrites[JSONProfileImage] = (
      (JsPath \ "typeOf").write[String] and
        (JsPath \ "id").writeNullable[UUID] and
        (JsPath \ "url").writeNullable[URL]
      )(unlift(JSONProfileImage.unapply))
  }

  implicit def fmt[T <: ProfileImage]: OFormat[ProfileImage] = new OFormat[ProfileImage] {

    def reads(json: JsValue): JsResult[ProfileImage] = {
      json.validate[JSONProfileImage].flatMap((jsonPI) => jsonPI.typeOf match {
        case "gravatar" =>
          jsonPI.url.map((url) => JsSuccess[ProfileImage](GravatarProfileImage(url))).get
        case "local" =>
          jsonPI.id.map((id) => JsSuccess[ProfileImage](LocalProfileImage(id))).get
      })
    }
    def writes(pi: ProfileImage) = Json.toJson(JSONProfileImage(pi)).as[JsObject]
  }
}

object ProfileImageType extends Enumeration {
  type ProfileImageType = Value
  val Local, Gravatar = Value

  def typeToString(t : ProfileImageType.Value) : String = t match {
    case ProfileImageType.Gravatar => "gravatar"
    case ProfileImageType.Local => "local"
  }
}