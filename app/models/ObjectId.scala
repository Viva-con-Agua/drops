package models

import play.api.libs.json.Json

case class ObjectId($oid : String)

object ObjectId {
  implicit val idFormat = Json.format[ObjectId]
}

case class ObjectIdWrapper(_id: ObjectId)

object ObjectIdWrapper {
  implicit val idWrapperFormat = Json.format[ObjectIdWrapper]
}