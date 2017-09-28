package civiretention

import java.net.URI

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import play.Play
import javax.inject._

import civiretention.CiviApi.CiviResult
import play.api.libs.json._

import scala.concurrent.Future
import play.api._
import play.api.i18n.MessagesApi
import play.api.libs.ws._
import play.api.libs.functional.syntax._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.i18n.Messages.Implicits._
import play.api.libs.ws.ning.NingWSClient

trait CiviApi {
  def get[T](entity: String, params: Map[String, String] = Map())(implicit rds: Reads[T], messages: Messages): Future[List[T]]

  def create[T](obj: T, entity: String, params: Map[String, String] = Map())(implicit rds: Reads[T], wts: T => Map[String, String], messages: Messages): Future[List[T]]

  def delete[T](id: String, entity: String, params: Map[String, String] = Map())(implicit rds: Reads[T], messages: Messages): Future[Boolean]

  def update[T](obj: T, entity: String, params: Map[String, String] = Map())(implicit rds: Reads[T], wts: T => Map[String, String], messages: Messages): Future[List[T]]
}

/**
  * Created by johann on 12.09.17.
  */
class CiviApiImpl @Inject() (ws: WSClient, configuration: Configuration, messagesApi: MessagesApi) extends CiviApi {
  val uriOpt = configuration.getString("civicrm.url").map(new URI( _ ))
  val keyOpt = configuration.getString("civicrm.key")
  val apiKeyOpt = configuration.getString("civicrm.apiKey")
  val debug = 0
  val json = 1
  val version = 3

  override def get[T](entity: String, params: Map[String, String] = Map())(implicit rds: Reads[T], messages: Messages) : Future[List[T]] =
    this.call(entity, "get", params).map(_.validate[CiviResult[T]].asOpt).map(_ match {
      case Some(result) => result.values
      case _ => Nil
    })

  override def create[T](obj: T, entity: String, params: Map[String, String])(implicit rds: Reads[T], wts: T => Map[String, String], messages: Messages): Future[List[T]] =
    this.call(entity, "create", params ++ wts(obj)).map(_.validate[CiviResult[T]].asOpt).map(_ match {
      case Some(result) => result.values
      case _ => Nil
    })

  override def delete[T](id: String, entity: String, params: Map[String, String])(implicit rds: Reads[T], messages: Messages): Future[Boolean] =
    this.call(entity, "delete", params ++ Map("id" -> id)).map(_.validate[CiviResult[T]].asOpt).map(_ match {
      case Some(result) => result.is_error == 0
      case _ => false
    })

  override def update[T](obj: T, entity: String, params: Map[String, String])(implicit rds: Reads[T], wts: (T) => Map[String, String], messages: Messages): Future[List[T]] =
    this.call(entity, "replace", params ++ wts(obj)).map(_.validate[CiviResult[T]].asOpt).map(_ match {
      case Some(result) => result.values
      case _ => Nil
    })

  private def call(entity: String, method: String, params: Map[String, String])(implicit messages: Messages) : Future[JsValue] = {
    val uri = if(uriOpt.isDefined) {
      uriOpt.get
    } else {
      throw CiviConnectionError(Messages("civi.crm.error.config.noURI"))
    }
    val key = if(keyOpt.isDefined) {
      keyOpt.get
    } else {
      throw CiviConnectionError(Messages("civi.crm.error.config.noKey"))
    }
    val apiKey = if(apiKeyOpt.isDefined) {
      apiKeyOpt.get
    } else {
      throw CiviConnectionError(Messages("civi.crm.error.config.noApiKey"))
    }
    val queryString = Map(
      "key" -> key,
      "api_key" -> apiKey,
      "debug" -> this.debug.toString,
      "json" -> this.json.toString,
      "version" -> this.version.toString,
      "entity" -> entity,
      "action" -> method
    ) ++ params

    val request: WSRequest = ws.url(uri.toURL.toString)
      .withHeaders("Accept" -> "application/json")
      .withRequestTimeout(3000)
      .withQueryString(queryString.toSeq: _*)

    request.get().map(_.json).recover {
      case ex: Exception => {
        Logger.error(ex.getMessage)
        throw CiviConnectionError(Messages("civi.crm.error.network"))
      }
    }
  }
}

object CiviApi {
  case class CiviResult[T](is_error: Int, undefined_fields: List[String], version: Int, count: Int, id: Option[Int], values: List[T])

  implicit def fmt[T](implicit fmt: Reads[T]): Reads[CiviResult[T]] = (
    (JsPath \ "is_error").read[Int] and
      (JsPath \ "undefined_fields").readNullable[List[String]] and
      (JsPath \ "version").read[Int] and
      (JsPath \ "count").read[Int] and
      (JsPath \ "id").readNullable[Int] and
      (JsPath \ "values").read[List[T]]
    ).tupled.map((t) => CiviResult(t._1, t._2.getOrElse(Nil), t._3, t._4, t._5, t._6))
}