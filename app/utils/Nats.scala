package utils

import java.util.{Properties, UUID}
import org.nats._
import play.api.mvc._
import javax.inject.Inject
import play.api.Configuration
import java.util._

class Nats @Inject() (
  configuration : Configuration
  )
{
  
  val server = configuration.getString("nats.ip").get
  val opts: Properties = new Properties
  opts.put("servers", server)

  def publishLogout(publicId : UUID){
    //val opts: Properties = new Properties
    //opts.put("server", server)
    val conn = Conn.connect(opts)
    conn.publish("LOGOUT", publicId.toString)
    conn.close
  }

  def publishCreate(model: String, publicId: UUID) {
    val conn = Conn.connect(opts)
    val key = model + ".CREATE"
    conn.publish(key, publicId.toString)
    conn.close
  }

  def publishUpdate(model: String, publicId: UUID) {
    val conn = Conn.connect(opts)
    val key = model + ".UPDATE"
    conn.publish(key, publicId.toString)
    conn.close
  }

  def publishDelete(model: String, publicId: UUID) {
    val conn = Conn.connect(opts)
    val key = model + ".DELETE"
    conn.publish(key, publicId.toString)
    conn.close
  }

 
}
