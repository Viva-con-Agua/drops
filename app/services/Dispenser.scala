package service

import play.api.mvc._
import play.api.Configuration
import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.twirl.api.Html

import java.util._
import javax.inject.Inject

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class Dispenser @Inject() (
  configuration: Configuration,
  ws: WSClient
) extends Controller {
  
  val dispenserUrl = configuration.getConfig("dispenser.ip") 

  def connect(url: String, json: JsValue): Future[WSResponse] = {
    ws.url(url)
      .withHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000)
      .post(json)
  }

  def getNavigation(navigation: String): Html = {
    val navigationJson: JsValue = Json.obj("navigation" -> navigation)
    val url = dispenserUrl + "navigation/get/" + navigation
    val navigationString: Future[Html] = connect(url, navigationJson).map { response => 
        Html(response.body)
      }
    Await.result(navigationString, 60 second)

  }

}
