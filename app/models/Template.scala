package models.dispenser

import java.util.UUID
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, OWrites, Reads}
import scala.concurrent.ExecutionContext.Implicits.global

case class MetaData (
  msName: String,
  templateName: String,
  language: Option[List[String]]
)

object MetaData {
  
  implicit val metaDataWrites : OWrites[MetaData] = (
    (JsPath \ "msName").write[String] and
    (JsPath \ "templateName").write[String] and
    (JsPath \ "language").writeNullable[List[String]]
  )(unlift(MetaData.unapply))
  implicit val metaDataReads : Reads[MetaData] = (
    (JsPath \ "msName").read[String] and 
    (JsPath \ "templateName").read[String] and
    (JsPath \ "language").readNullable[List[String]]
  )(MetaData.apply _ )
}
case class TemplateData (
  title: String,
  content: String
)

object TemplateData {

  implicit val templateDataWrites: OWrites[TemplateData] = (
    (JsPath \ "title").write[String] and
    (JsPath \ "content").write[String]
  )(unlift(TemplateData.unapply))
  implicit val templateDataReads: Reads[TemplateData] = (
    (JsPath \ "title").read[String] and
    (JsPath \ "content").read[String] 
  )(TemplateData.apply _)
}

case class NavigationData (
  navigationName: String,
  active: String,
  user_id: Option[UUID]
)

object NavigationData {
  implicit val navigationDataWrites: OWrites[NavigationData] = (
    (JsPath \ "navigationName").write[String] and
    (JsPath \ "active").write[String] and
    (JsPath \ "user_id").writeNullable[UUID]
  )(unlift(NavigationData.unapply))
  implicit val navigationDataReads: Reads[NavigationData] = (
    (JsPath \ "navigationName").read[String] and
    (JsPath \ "active").read[String] and
    (JsPath \ "user_id").readNullable[UUID]
  )(NavigationData.apply _)
}

case class Template (
  metaData: MetaData,
  navigationData: NavigationData,
  templateData: TemplateData
)

object Template {

  implicit val templateWrites: OWrites[Template] = (
    (JsPath \ "metaData").write[MetaData] and
    (JsPath \ "navigationData").write[NavigationData] and
    (JsPath \ "templateData").write[TemplateData]
  )(unlift(Template.unapply))
  implicit val templateReads: Reads[Template] = (
    (JsPath \ "metaData").read[MetaData] and
    (JsPath \ "navigationData").read[NavigationData] and
    (JsPath \ "templateData").read[TemplateData]
  )(Template.apply _)
}

object JsonFormatsTemplate {
  import play.api.libs.json.Json

  implicit val metaDataFormat = Json.format[MetaData]
  implicit val templateDataFormat = Json.format[TemplateData]
  implicit val navigationDataFormat = Json.format[NavigationData]
  implicit val templateFormat = Json.format[Template]
}

