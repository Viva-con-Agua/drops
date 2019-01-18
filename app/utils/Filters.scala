package utils

import javax.inject.Inject

import scala.concurrent.Future
import play.api._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._
import Results.Ok
import play.api.http.HttpFilters
import play.filters.csrf.CSRFFilter

// If we configure play.http.forwarded.trustedProxies correctly, we don't need this filter... right? right!?
/*
class TrustXForwardedFilter extends Filter {
  def apply(nextFilter:RequestHeader => Future[Result])(request:RequestHeader):Future[Result] = {
    val newRequest = request.headers.get("X-Forwarded-Proto").collect {
       case "https" => request.copy(secure=true)
    }.getOrElse(request)
    nextFilter(newRequest)
  }
} 
*/

/**
 * There's no way (that I know) to turn off http in Bluemix, hence this filter.
 * Enable in production with application.proxy.httpsOnly=true.
 */

class HttpsOnlyFilter @Inject() (val messagesApi:MessagesApi) extends Filter with I18nSupport {
  def apply(nextFilter:RequestHeader => Future[Result])(request:RequestHeader):Future[Result] = {
    implicit val r = request
    request.headers.get("X-Forwarded-Proto").map {
      case "https" => nextFilter(request)
      case _ => Future.successful(Ok(""))
    }.getOrElse(nextFilter(request))
  }
}

/**
  * Implement exceptions for the globally applied CSRF filter. Needed for POST requests without a session, like OAuth2
  * Access Token requests and maybe some REST endpoints.
  *
  * @author Johann Sell
  */
class CSRFExceptionsFilter extends EssentialFilter {
  def apply(action: EssentialAction) = new EssentialAction {
    def apply(rh: RequestHeader) = {
      val newHeaders =
        if (rh.path.startsWith(controllers.routes.OAuth2Controller.accessToken.url)) {
          rh.copy(headers = rh.headers.add("Csrf-Token" -> "nocheck"))
        } else {
          rh
        }
      action(newHeaders)
    }
  }
}

class Filters @Inject() (
    configuration:Configuration, 
    csrfFilter:CSRFFilter,
    csrfExceptionFilter: CSRFExceptionsFilter,
    httpsOnlyFilter:HttpsOnlyFilter) extends HttpFilters {
  
  val map = Map("application.proxy.httpsOnly" -> httpsOnlyFilter)
  override def filters = csrfExceptionFilter +: csrfFilter +: map.foldRight[Seq[EssentialFilter]](Seq.empty) { case ((k,f),filters) =>
    configuration.getBoolean(k) collect {
      case true => f +: filters
    } getOrElse filters
  }
}
