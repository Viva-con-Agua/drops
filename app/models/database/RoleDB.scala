package models.database

import play.api.libs.json.{JsPath, Reads, _}
import play.api.libs.functional.syntax._

case class RoleDB (
  id: Long,
  name: String
  )

object RoleDB{
  def apply(tuple: (Long, String)): RoleDB =
    RoleDB(tuple._1, tuple._2)

  def mapperTo(id: Long, name: String) = apply(id, name)

  implicit val roleDBBWrites : OWrites[RoleDB] = (
    (JsPath \ "id").write[Long]and
      (JsPath \ "name").write[String]
    )(unlift(RoleDB.unapply))

  implicit val roleDBReads : Reads[RoleDB] = (
    (JsPath \ "id").read[Long]and
      (JsPath \ "name").read[String]
    ).tupled.map((role) => RoleDB(role))
}
