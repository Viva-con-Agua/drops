package models

import java.net.URL

import play.api.libs.json._

/**
  * Created by johann on 08.09.17.
  */
object URLHelper {
  implicit val urlJSONRead = new Reads[URL] {
    override def reads(json: JsValue): JsResult[URL] = json.validate[String].map(new URL( _ ))
  }

  implicit val urlJSONWrites = new Writes[URL] {
    override def writes(o: URL): JsValue = JsString(o.toURI.toASCIIString)
  }
}
