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
  
  val server = configuration.getConfig("nats.ip")
  val opts: Properties = new Properties
  opts.put("server", server)

  def publishLogout(publicId : UUID){
    //val opts: Properties = new Properties
    //opts.put("server", server)
    val conn = Conn.connect(opts)
    conn.publish("LOGOUT", publicId.toString)
    conn.close
  }

  def publishCreate(model: String, publicId: UUID) {
    val conn = Conn.connect(opts)
    val msg = model + " " + publicId.toString
    conn.publish("CREATE", msg)
    conn.close
  }

  def publishUpdate(model: String, publicId: UUID) {
    val conn = Conn.connect(opts)
    val msg = model + " " + publicId.toString
    conn.publish("UPDATE", msg)
    conn.close
  }

  def publishDelete(model: String, publicId: UUID) {
    val conn = Conn.connect(opts)
    val msg = model + " " + publicId.toString
    conn.publish("DELETE", msg)
    conn.close
  }

 
}
