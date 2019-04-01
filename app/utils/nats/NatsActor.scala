package utils.nats

import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import io.nats.client.Subscription
import akka.actor._
import javax.inject._
import java.time.Duration
import java.util.concurrent.CountDownLatch
import play.api.libs.concurrent.InjectedActorSupport


object NatsParentActor {
  case class GetChild(key: String)
}

class NatsParentActor @Inject() (
  childFactory: NatsPublishActor.Factory
  ) extends Actor with InjectedActorSupport {
  import NatsParentActor._
  
  def receive = {
    case GetChild(key: String) =>
      val child: ActorRef = injectedChild(childFactory(key), key)
      sender() ! child
  }
}

object NatsPublishActor {
 
  def props = Props[NatsPublishActor]
  case class Publish(address: String, sub: String, id: String)

  trait Factory {
    def apply(key: String): Actor
  }
}

class NatsPublishActor extends Actor {
  import NatsPublishActor._

  def receive = {
    case Publish(address: String, sub: String, id: String) => {
      try {
        val connection: Connection = Nats.connect(address: String)
        connection.publish(sub, id.toCharArray.map(_.toByte))
        connection.flush(Duration.ZERO)
        connection.close
        sender() ! "Publish " + sub + " " + id
      } catch {
        case e: Exception => sender() ! e.toString
      }
    }
  }
}

object NatsSubscribeActor {
  def props = Props[NatsSubscribeActor]
  case class Subscribe(address: String, sub: String, id: String)
}

class NatsSubscribeActor extends Actor {
  import NatsSubscribeActor._

  def receive = {
    case Subscribe(address: String, sub: String, id: String) => {
      try {
        val connection: Connection = Nats.connect(address: String)
        val subscription : Subscription = connection.subscribe(sub, id)
        sender() ! subscription.nextMessage(Duration.ZERO).getSubject()
      } catch {
        case e: Exception => sender() ! e.toString
      }

    }
  }

}




