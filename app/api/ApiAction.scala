package api

import javax.inject.Inject

import play.api._
import play.api.Logger
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc._
import play.api.mvc.Results._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by johann on 21.12.16.
  */
class ApiAction @Inject()(
 val messagesApi: MessagesApi,
 apiRequestProvider : ApiRequestProvider
) extends ActionBuilder[ApiRequest] {
  private val logger = Logger(Action.getClass)

  def invokeBlock[A](request: Request[A], block: (ApiRequest[A]) => Future[Result]) = {
    implicit val messages : Messages = messagesApi.preferred(request)
    Try(apiRequestProvider.get[A](request)) match {
      case Success(apiRequest) => block(apiRequest)/*apiRequest.getClient.flatMap(_ match {
        case Some(oauthClient) => block(apiRequest)
        case _ => Future.successful(BadRequest(Json.obj("error" -> Messages("rest.api.noValidAPIClient"))))
      })*/
      case Failure(f) => Future.successful(BadRequest(Json.obj("error" -> Messages("rest.api.noValidAPIRequest", f.getMessage))))
    }
  }
}
