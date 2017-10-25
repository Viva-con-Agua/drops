package services

import scala.io.Source
import scala.concurrent.Future
import play.api.libs.json.{JsPath, JsValue, Json, Reads}
import play.api.libs.ws._
import play.api.cache._
import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try, Success, Failure}

import play.twirl.api.Html


class TemplateHandler @Inject() (
    ws: WSClient,
    cache: CacheApi) {
    
    def loadJson (json: String) : Try[String] = {
      Try(Source.fromFile("app/assets/jsons/" + json + ".json").getLines.mkString)
    }
     
      
      
    def responseHandler (template : String) : String = {
      var json: JsValue = Json.parse({""""""}) 
      loadJson(template) match {
        case Success(j) => json = Json.parse(j)
        case Failure(f) => println(f)
      }
      val request : WSRequest = ws.url("http://172.17.0.3:9000/getTemplate")
        .withHeaders("Accept" -> "application/json")
        .withRequestTimeout(10)
      val templateResponse: Future[WSResponse] = request.post(json)
      var body = ""
      templateResponse.map({ response =>
        body = response.body
      })
      println(body)
      body
      
    }
   
    def getTemplate (id: String) : String = {
      responseHandler(id)
    }
    /*
    def getTemplate (id: String) : String = {
      val template : String = cache.getOrElse[String](id){
        val temp : String = responseHandler(id)
        cache.set(id, temp)
        temp
      }
      template

    }
  **/
}

