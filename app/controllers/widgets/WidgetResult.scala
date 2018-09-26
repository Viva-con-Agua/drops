package controllers.widgets

import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{RequestHeader, Result}

class WidgetResult(request: RequestHeader, code: play.api.mvc.Results.Status, msg: String, msgValues: List[String], internalStatusCode: String, additional: JsValue) {

  /**
    * Generates an HTTP result containing the status as JSON encoded object.
    *
    * @author Johann Sell
    * @param messagesApi
    * @return
    */
  def getResult(implicit messagesApi: MessagesApi, messages: Messages) : Result =
    request.accepts("application/json") match {
      case true => code(getStatus).as("application/json")
      case _ => generateStatusMsg(request, code, msg, additional)
    }

  /**
    * Encodes the status as JSON.
    *
    * @author Johann Sell
    * @param messagesApi
    * @return
    */
  def getStatus(implicit messagesApi: MessagesApi, messages: Messages) : JsValue = Json.obj(
    "internal_status_code" -> (internalStatusCode),
    "http_status_code" -> code.header.status,
    "msg_i18n" -> msg,
    "msg" -> Messages(msg, msgValues:_*),
    "additional_information" -> additional
  )

  private def generateStatusMsg(request: RequestHeader, code: play.api.mvc.Results.Status, msg: String, additional: JsValue)(implicit messages: Messages) : Result =
    code(Messages(msg))
}

object WidgetResult {

  case class Generic( request: RequestHeader,  code: play.api.mvc.Results.Status,  msg: String,  msgValues: List[String],  internalStatusCode: String,  additional: JsValue) extends
    WidgetResult(request, code, msg, msgValues, internalStatusCode, additional)

  object Generic {
    def apply(request: RequestHeader, code: play.api.mvc.Results.Status, msg: String, msgValues: List[String], internalStatusCode: String, additional: Map[String, String] = Map()): Generic =
      Generic(request, code, msg, msgValues, internalStatusCode, Json.toJson(additional))
  }

  case class Bogus( request: RequestHeader,  msg: String,  msgValues: List[String],  internalStatusCode: String, errors: JsValue) extends
    WidgetResult(request, play.api.mvc.Results.BadRequest, msg, msgValues, internalStatusCode, errors)


  case class NotFound( request: RequestHeader,  msg: String,  msgValues: List[String],  internalStatusCode: String, additionalMap : Map[String, String]) extends
    WidgetResult(request, play.api.mvc.Results.NotFound, msg, msgValues, internalStatusCode, Json.toJson(additionalMap))

  case class Forbidden( request: RequestHeader,  msg: String,  msgValues: List[String],  internalStatusCode: String, additionalMap : Map[String, String]) extends
    WidgetResult(request, play.api.mvc.Results.Forbidden, msg, msgValues, internalStatusCode, Json.toJson(additionalMap))

  case class Unauthorized( request: RequestHeader,  msg: String,  msgValues: List[String],  internalStatusCode: String, additionalMap : Map[String, String]) extends
    WidgetResult(request, play.api.mvc.Results.Unauthorized, msg, msgValues, internalStatusCode, Json.toJson(additionalMap))


  case class Ok( request: RequestHeader,  msg: String,  msgValues: List[String],  internalStatusCode: String,  additional : JsValue) extends
    WidgetResult(request, play.api.mvc.Results.Ok, msg, msgValues, internalStatusCode, additional)

  object Ok {
    def apply(request: RequestHeader, msg: String, msgValues: List[String], internalStatusCode: String, additional : Map[String, String] = Map()) : Ok =
      Ok(request, msg, msgValues, internalStatusCode, Json.toJson(additional))
  }
}