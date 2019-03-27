package utils

import java.util.{Properties, UUID}
import org.nats._
import play.api.mvc._
import javax.inject.Inject
import play.api.{Configuration, Logger}
import akka.actor._
import java.util._
import java.net.InetSocketAddress
import play.api.libs.concurrent.Execution.Implicits.defaultContext
//import scakla.concurrent.duration._
import utils.nats.NatsPublishActor
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Future

class NatsController @Inject() (
  configuration : Configuration,
  system: ActorSystem
  )
{
  
import scala.concurrent.duration._
implicit val timeout: Timeout = 5000.millis


  val server = configuration.getString("nats.ip").get
 // val opts: Properties = new Properties
 // opts.put("servers", server)
  
  val natsPublishActor = system.actorOf(NatsPublishActor.props, "publish-actor")


  def publishLogout(publicId : UUID): Future[String] = {
      import utils.nats._
      natsPublishActor.ask(NatsPublishActor.Publish(server, "LOGOUT", publicId.toString)).mapTo[String].map( message => message)
  }
//
//  def publishCreate(model: String, publicId: UUID) {
//    val conn = Conn.connect(opts)
//    val key = model + ".CREATE"
//    conn.publish(key, publicId.toString)
//    conn.close
//      natsPublishActor.ask(NatsPublishActor.mapTo[String].map { message =>
//        Logger.debug(message)
//      }
//  }
//
//  def publishUpdate(model: String, publicId: UUID) {
//    (natsClient ? c).mapTo[String].map { message =>
//        Logger.debug(message)
//      }
//    val conn = Conn.connect(opts)
//    val key = model + ".UPDATE"
//    conn.publish(key, publicId.toString)
//    conn.close
//  }
//
//  def publishDelete(model: String, publicId: UUID) {
//    val conn = Conn.connect(opts)
//    val key = model + ".DELETE"
//    conn.publish(key, publicId.toString)
//    conn.close
//    (natsClient ? c).mapTo[String].map { message =>
//        Logger.debug(message)
//      }
//  }

 
}
