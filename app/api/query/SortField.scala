package api.query

import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Created by johann on 13.01.17.
  */
case class SortField(field: String, dir: Sort)

object SortField {
  def apply(tuple: (String, String)) : SortField = SortField(tuple._1, Sort(tuple._2))

  implicit val sortFieldWrites: Writes[SortField] = (
    (__ \ 'field).write[String] and
      (__ \ 'dir).write[String]
    )(raw => (raw.field, raw.dir.dir))
  implicit val sortFieldReads : Reads[SortField] = (
    (JsPath \ "field").read[String] and
      (JsPath \ "dir").read[String]
    ).tupled.map(SortField( _ ))
}
