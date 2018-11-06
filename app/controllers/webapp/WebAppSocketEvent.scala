package controllers.webapp
import play.api.mvc.WebSocket.FrameFormatter
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json._
import java.util.UUID
import play.api.libs.functional.syntax._
import scala.util.control.NonFatal

case class GetSocketQuery(
  model: String,
  id: Option[UUID],
  name: Option[String]
  )
object GetSocketQuery{
  implicit val getSocketQueryFormat = Json.format[GetSocketQuery]
}

case class WebSocketEvent(
  operation: String,
  query: Option[List[JsValue]],
  status: Option[String]
  )

object WebSocketEvent {
  implicit val webSocketEventFormat = Json.format[WebSocketEvent]
  implicit val webSocketEventFormatter: FrameFormatter[WebSocketEvent] = implicitly[FrameFormatter[String]].transform(WebSocketEvent => WebSocketEvent.toString, { text =>
    try {
      val result: JsResult[WebSocketEvent] = Json.parse(text).validate[WebSocketEvent]
      result match {
        case s: JsSuccess[WebSocketEvent] => s.get
        case e: JsError => WebSocketEvent("ERROR", Option(List(JsError.toJson(e))), Option("the WebSocket require a WebSocketEvent object as Json"))
      }
    }catch {
      case NonFatal(e) => WebSocketEvent("ERROR", None, Option(e.getMessage))
    }
  })
}
