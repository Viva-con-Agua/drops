package utils

import java.util.{Properties, UUID}
import org.nats._
import play.api.mvc._
import javax.inject._
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
import scala.concurrent.duration._

@Singleton
class NatsController @Inject() (
  configuration : Configuration,
  system: ActorSystem
  )
{
  
  implicit val timeout: Timeout = 5000.millis
  val server = configuration.getString("nats.ip").get
  
  val natsPublishActor = system.actorOf(NatsPublishActor.props, "publish-actor")


  def publishLogout(publicId : UUID): Future[String] = {
      import utils.nats._
      natsPublishActor.ask(NatsPublishActor.Publish(server, "drops.user.logout", publicId.toString)).mapTo[String].map( message => message)
  }

  def publishCreate(model: String, publicId: UUID): Future[String] = {
    import utils.nats._
    natsPublishActor.ask(NatsPublishActor.Publish(server, "drops.user.created", publicId.toString)).mapTo[String].map( message => message)
  }

  def publishUpdate(model: String, publicId: UUID): Future[String] = {
    import utils.nats._
    natsPublishActor.ask(NatsPublishActor.Publish(server, "drops.user.updated", publicId.toString)).mapTo[String].map( message => message)
  }

  def publishDelete(model: String, publicId: UUID): Future[String] = {
    import utils.nats._
    natsPublishActor.ask(NatsPublishActor.Publish(server, "drops.user.deleted", publicId.toString)).mapTo[String].map( message => message)
  }
}
