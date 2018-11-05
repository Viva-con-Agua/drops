package controllers.webapp

import play.api.mvc._
import akka.actor.{Actor, ActorRef}
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import play.api.i18n.{Messages, MessagesApi}
import scala.language.postfixOps

class WebSocketActor (out: ActorRef) extends Actor {

  // the akka server context
  def system = play.api.libs.concurrent.Akka.system(play.api.Play.current)
  
  //send a message to all actors of the system context. 
  def broadcast(msg: WebSocketEvent) = system.actorSelection("akka://application/system/websockets/*/handler") ! msg
  
  //handle WebSocketEvent operators
  def handleWebSocketEvent(msg: WebSocketEvent): WebSocketEvent = {
   //lazy val responseTimestamp = currentTime
      msg.operation match {
        case "INSERT" => {
          val received = Await.result(insert(msg), 10 second)
          system.actorSelection("akka://application/system/websockets/*/handler") ! received
          received
        }
        case "UPDATE" => { 
          val received = Await.result(update(msg), 10 second)
          system.actorSelection("akka://application/system/websockets/*/handler") ! received
          received
        }
        case "DELETE" => 
          val received = Await.result(delete(msg), 10 second)
          system.actorSelection("akka://application/system/websockets/*/handler") ! received
          received

        case "SUCCESS" => msg
        case _ => notMatch(msg)
      }
  }
  
  def receive = {
    case request: WebSocketEvent =>
      val response = handleWebSocketEvent(request)
      out ! response
  }

  def insert(event: WebSocketEvent):Future[WebSocketEvent] = Future.successful(event)
  
  def update(event: WebSocketEvent):Future[WebSocketEvent] = Future.successful(event)

  def delete(event: WebSocketEvent):Future[WebSocketEvent] = Future.successful(event)

  def notMatch(event: WebSocketEvent): WebSocketEvent = event
}
