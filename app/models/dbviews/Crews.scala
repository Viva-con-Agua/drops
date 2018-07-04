package models.dbviews

import java.util.UUID

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, _}

case class Crews (
                   crew: Option[CrewView],
                   city: Option[CityView]
                 ) extends ViewObject {

  def getValue (viewname: String): ViewBase = {
    viewname match {
      case "city" => city.get
      case "crew" => crew.get
    }
  }

  def isFieldDefined(viewname: String): Boolean = {
    viewname match{
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
              name: Option[Map[String,String]]
            )extends ViewBase{
  def getValue (fieldname: String, index: Int): Object = {
    fieldname match {
      case "name" => name.get.get(index.toString).get
    }
  }

  def isFieldDefined(fieldname: String, index: Int): Boolean = {
    fieldname match{
      case "name" => {
        name.isDefined match {
          case true => name.get.keySet.contains(index.toString)
          case false => false
        }
      }
    }
  }
}
object CityView{
  implicit val cityViewWrites: Writes[CityView] =
    (JsPath \ "name").writeNullable[Map[String, String]].contramap(_.name)

  implicit val cityViewReads: Reads[CityView] =
    (JsPath \ "name").readNullable[Map[String,String]].orElse(
      (JsPath \ "name").readNullable[String].map(_.map(n => Map("0" -> n)))).map(CityView.apply)
}

case class CrewView(
                   publicId: Option[Map[String, UUID]],
                   name: Option[Map[String, String]],
                   country: Option[Map[String, String]]
               )extends ViewBase {
  def getValue (fieldname: String, index: Int): Object = {
    fieldname match {
      case "publicId" => publicId.get.get(index.toString).get //getOrElse?
      case "name" => name.get.get(index.toString).get
      case "country" => country.get.get(index.toString).get
    }
  }

  def isFieldDefined(fieldname: String, index: Int): Boolean = {
    fieldname match {
      case "publicId" => {
        publicId.isDefined match {
          case true => publicId.get.keySet.contains(index.toString)
          case false => false
        }
      }
      case "name" => {
        name.isDefined match {
          case true => name.get.keySet.contains(index.toString)
          case false => false
        }
      }
      case "country" => {
        country.isDefined match {
          case true => country.get.keySet.contains(index.toString)
          case false => false
        }
      }
    }
  }
}

object CrewView{
  def apply(tuple: (Option[Map[String, UUID]], Option[Map[String, String]], Option[Map[String, String]])): CrewView =
    CrewView(tuple._1, tuple._2, tuple._3)

  implicit val crewViewWrites : OWrites[CrewView] = (
    (JsPath \ "publicId").writeNullable[Map[String, UUID]] and
      (JsPath \ "name").writeNullable[Map[String, String]] and
      (JsPath \ "country").writeNullable[Map[String, String]]
    )(unlift(CrewView.unapply))

  implicit val crewViewReads : Reads[CrewView] = (
    (JsPath \ "publicId").readNullable[Map[String, UUID]].orElse(
      (JsPath \ "publicId").readNullable[UUID].map(_.map(p => Map("0" -> p))))and
      (JsPath \ "name").readNullable[Map[String, String]].orElse(
        (JsPath \ "name").readNullable[String].map(_.map(n => Map("0" -> n))))and
      (JsPath \ "country").readNullable[Map[String, String]].orElse(
          (JsPath \ "country").readNullable[String].map(_.map(c => Map("0" -> c))))
    ).tupled.map(CrewView( _ ))
}