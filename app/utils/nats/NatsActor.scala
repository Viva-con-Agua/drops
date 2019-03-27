package utils.nats

import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import akka.actor._
import javax.inject._
import java.time.Duration

object NatsPublishActor {
 
  def props = Props[NatsPublishActor]
  case class Publish(address: String, event: String, id: String)
}

class NatsPublishActor extends Actor {
  import NatsPublishActor._

  def receive = {
    case Publish(address: String, event: String, id: String) => {
      try {
        val connection: Connection = Nats.connect(address: String)
        connection.publish(event, id.toCharArray.map(_.toByte))
        connection.flush(Duration.ZERO)
        connection.close
        sender() ! "Publish " + event + " " + id
      } catch {
        case e: Exception => sender() ! e.toString
      }
      sender() ! ""
    }
  }
}



object HelloActor {
  def props = Props[HelloActor]
  
  case class SayHello(name: String)
}

class HelloActor extends Actor {
  import HelloActor._
  
  def receive = {
    case SayHello(name: String) =>
      sender() ! "Hello, " + name
  }
}
