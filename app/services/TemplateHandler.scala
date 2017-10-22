package services

import scala.io.Source
import scala.concurrent.Future
import play.api.libs.json.{JsPath, JsValue, Json, Reads}
import play.api.libs.ws._
import play.api.cache._
import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global

object TemplateHtml {
  
  class TemplateHandler @Inject() (
    ws: WSClient,
    cache: CacheApi) {
    
    def responseHandler (template : String) : String = {
      val source: String = Source.fromFile("app/assets/jsons/" + template + ".json").getLines.mkString
      val json: JsValue = Json.parse(source)
      val request : WSRequest = ws.url("http://localhost:4000")
      val requestTemplate : WSRequest = request.withHeaders("Accept" -> "application/json")
      val templateResponse: Future[WSResponse] = requestTemplate.post(json)
      var body = ""
      templateResponse.map({ response =>
        body = response.body
      })
      body
    }
    
    def getTemplate (id: String) : String = {
      val template : String = cache.getOrElse[String](id){
        val temp : String = responseHandler(id)
        cache.set(id, temp)
        temp
      }
      template

    }
  }
}
