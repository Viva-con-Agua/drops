package persistence.pool1

import java.net.URI
import javax.inject.Inject

import java.util.UUID
import play.api.i18n.{Messages, MessagesApi}
import play.api.{Configuration, Logger}
import play.api.libs.ws._
import play.api.http.Writeable._
import play.api.i18n.Messages

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class PoolApi @Inject() (ws: WSClient, configuration: Configuration, messageApi: MessagesApi) {
  private val uriOpt = configuration.getString("pool1.url").map(new URI( _ ))
  private val logoutUrl = configuration.getString("pool1.logouturl")
  private val hash = configuration.getString("pool1.hash")

  def create[T](data: PoolData[T])(implicit messages: Messages): Future[Either[PoolResult, Exception]] = {
    try {
      val uri = if (uriOpt.isDefined)
        uriOpt.get
      else
        throw new Exception(Messages("pool1.error.config.noURI"))
      val request: WSRequest = ws.url(uri.toURL.toString)
        .withHeaders("Accept" -> "application/json")
        .withFollowRedirects(true)
        .withRequestTimeout(3000)

      request.post(data.toPost).map((res) => {
        Left(res.json.validate[PoolResult].get)
      })
    } catch {
      case e: Exception => {
        Future.successful(Right(e))
      }
    }
  }

  def logout[T](userId: UUID)(implicit messages: Messages): Future[Either[PoolResult, Exception]] = {
    try {
      val url = if (logoutUrl.isDefined)
        logoutUrl.get
      else
        throw new Exception(Messages("pool1.error.config.noURI"))
      val request: WSRequest = ws.url(url)
        .withHeaders("Accept" -> "application/json")
        .withFollowRedirects(true)
        .withRequestTimeout(3000)
      
      request.post(userId.toString).map((res) => {
        Left(res.json.validate[PoolResult].get)
      })
    } catch {
      case e: Exception => {
        Future.successful(Right(e))
      }
    }
  }
}
