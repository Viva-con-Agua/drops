package controllers.webapp
import play.api.mvc.WebSocket.FrameFormatter
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.{JsValue, Json}


case class WebSocketEvent(
  operation: String,
  query: Option[List[JsValue]],
  status: Option[String]
  )

object WebSocketEvent {
  implicit val webSocketEventFormat = Json.format[WebSocketEvent]
  implicit val webSocketEventFormatter = FrameFormatter.jsonFrame[WebSocketEvent]
}

//case class CrewWebSocketEvent(override val query: Option[List[CrewStub]]) extends WebSocketEvent {}

