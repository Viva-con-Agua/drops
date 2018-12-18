package persistence.pool1

import java.net.URI
import javax.inject.Inject
import play.api.libs.json._
import models.User
import java.util.UUID
import play.api.i18n.{Messages, MessagesApi}
import play.api.{Configuration, Logger}
import play.api.libs.ws._
import play.api.http.Writeable._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class PoolApi @Inject() (ws: WSClient, configuration: Configuration, messageApi: MessagesApi) {
//  private val uriOpt = configuration.getString("pool1.url").map(new URI( _ ))
//  private val logoutUrl = configuration.getString("pool1.logouturl")
  private val hash = configuration.getString("pool1.hash")

  private def getConf(path: String) : Option[String] = configuration.getString("pool1.base").flatMap(url =>
    configuration.getString("pathes." + path).map(url + _)
  )

  private def request(url: String, data: PoolRequest)(implicit messages: Messages): Future[Either[PoolResponseData, Exception]] = {
    ws.url(url)
      .withHeaders("Accept" -> "application/json")
      .withFollowRedirects(true)
      .withRequestTimeout(3000)
      .post(data.toPost)
      .map(
      _.json.validate[PoolResponseData].map(Left( _ )).getOrElse(Right( new PoolException(Messages("pool1.api.canNotReadResponse"))))
    )
  }

  def create(data: PoolRequestUserData)(implicit messages: Messages): Future[Either[PoolResponseData, Exception]] = {
    getConf("create") match {
      case Some(createURL) => this.request(createURL, data)
      case None => Future.successful(Right(new PoolException(Messages("pool1.error.config.noURI"))))
    }
//    try {
//      val uri = if (uriOpt.isDefined)
//        uriOpt.get
//      else
//        throw new Exception(Messages("pool1.error.config.noURI"))
//      val request: WSRequest = ws.url(uri.toURL.toString)
//        .withHeaders("Accept" -> "application/json")
//        .withFollowRedirects(true)
//        .withRequestTimeout(3000)
//
//      request.post(data.toPost).map((res) => {
//        Left(res.json.validate[PoolResult].get)
//      })
//    } catch {
//      case e: Exception => {
//        Future.successful(Right(e))
//      }
//    }
  }

  def read(data: PoolRequestUUIDData)(implicit messages: Messages): Future[Either[PoolResponseData, Exception]] = {
    getConf("read") match {
      case Some(readURL) => this.request(readURL, data)
      case None => Future.successful(Right(new PoolException(Messages("pool1.error.config.noURI"))))
    }
  }

  def update(data: PoolRequestUserData)(implicit messages: Messages): Future[Either[PoolResponseData, Exception]] = {
    getConf("update") match {
      case Some(updateURL) => this.request(updateURL, data)
      case None => Future.successful(Right(new PoolException(Messages("pool1.error.config.noURI"))))
    }
  }

  def delete(data: PoolRequestUUIDData)(implicit messages: Messages): Future[Either[PoolResponseData, Exception]] = {
    getConf("delete") match {
      case Some(deleteURL) => this.request(deleteURL, data)
      case None => Future.successful(Right(new PoolException(Messages("pool1.error.config.noURI"))))
    }
  }

  def logout(data: PoolRequestUUIDData)(implicit messages: Messages): Future[Either[PoolResponseData, Exception]] = {
    getConf("logout") match {
      case Some(logoutURL) => this.request(logoutURL, data)
      case None => Future.successful(Right(new PoolException(Messages("pool1.error.config.noURI"))))
    }
//    try {
//      val url = if (logoutUrl.isDefined)
//        logoutUrl.get
//      else
//        throw new Exception(Messages("pool1.error.config.noURI"))
//      val request: WSRequest = ws.url(url)
//        .withHeaders("Accept" -> "application/json")
//        .withFollowRedirects(true)
//        .withRequestTimeout(3000)
//
//      request.post(data.toUUIDPost).map((res) => {
//        Left(res.json.validate[PoolResult].get)
//      })
//    } catch {
//      case e: Exception => {
//        Future.successful(Right(e))
//      }
//    }
  }

  /*private def toPoolPost(hash: String, user: User) : Map[String, Seq[String]] = {
    val uuidJson = user.id.toString
      Map(
        "hash" -> Seq(hash),
        "user" -> Seq(Json.obj({"UUID": uuidJson}).toString)
      )
    }*/

}
