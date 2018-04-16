package models.views

import java.util.UUID

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, _}

case class Crews (
                   crew: Option[CrewView],
                   city: Option[CityView]
                 ) extends ViewBase{

  //ToDo: Generic Solution? Perhaps with Scala Reflections
  def getValue (fieldname: String): Object = {
    fieldname match {
      case "city" => city.get
      case "crew" => crew.get
    }
  }

  def isFieldDefined(fieldname: String): Boolean = {
    fieldname match{
      case "city" => city.isDefined
      case "crew" => crew.isDefined
    }
  }
}

object Crews{
  def apply(tuple: (Option[CrewView], Option[CityView])) : Crews =
    Crews(tuple._1, tuple._2)

  implicit val crewsWrites : OWrites[Crews] =  (
    (JsPath \ "crew").writeNullable[CrewView] and
      (JsPath \ "city").writeNullable[CityView]
  )(unlift(Crews.unapply))

  implicit val crewsReads: Reads[Crews] = (
    (JsPath \ "crew").readNullable[CrewView] and
      (JsPath \ "city").readNullable[CityView]
  ).tupled.map(Crews( _ ))
}

case class CityView(
              name: Option[String]
            )extends ViewBase{
  def getValue (fieldname: String): Object = {
    fieldname match {
      case "name" => name.get
    }
  }

  def isFieldDefined(fieldname: String): Boolean = {
    fieldname match{
      case "name" => name.isDefined
    }
  }
}
object CityView{
  implicit val cityViewFormat: Format[CityView] = Json.format[CityView]
}

case class CrewView(
                   publicId: Option[UUID],
                   name: Option[String],
                   country: Option[String]
               )extends ViewBase {
  def getValue (fieldname: String): Object = {
    fieldname match {
      case "publicId" => publicId.get
      case "name" => name.get
      case "country" => country.get
    }
  }

  def isFieldDefined(fieldname: String): Boolean = {
    fieldname match {
      case "publicId" => publicId.isDefined
      case "name" => name.isDefined
      case "country" => country.isDefined
    }
  }
}

object CrewView{
  def apply(tuple: (Option[UUID], Option[String], Option[String])): CrewView =
    CrewView(tuple._1, tuple._2, tuple._3)

  implicit val crewViewWrites : OWrites[CrewView] = (
    (JsPath \ "publicId").writeNullable[UUID] and
      (JsPath \ "name").writeNullable[String] and
      (JsPath \ "country").writeNullable[String]
    )(unlift(CrewView.unapply))

  implicit val crewViewReads : Reads[CrewView] = (
    (JsPath \ "publicId").readNullable[UUID] and
      (JsPath \ "name").readNullable[String] and
      (JsPath \ "country").readNullable[String]
    ).tupled.map(CrewView( _ ))
}