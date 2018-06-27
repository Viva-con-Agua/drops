package models.dbviews

import java.text.SimpleDateFormat
import java.util.Date

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, _}

case class Tasks (
                    task: Option[TaskView],
                    accessRight: Option[AccessRightView]
                 )extends ViewObject {
  def getValue(viewname: String) : ViewBase = {
    viewname match {
      case "task" => task.get
      case "accessRight" => accessRight.get
    }
  }

  def isFieldDefined(viewname: String): Boolean =  {
    viewname match{
      case "task" => task.isDefined
      case "accessRight" => accessRight.isDefined
    }
  }
}

object Tasks{
  def apply(tuple: (Option[TaskView], Option[AccessRightView])) : Tasks =
    Tasks(tuple._1, tuple._2)

  implicit val tasksWrites : OWrites[Tasks] =  (
    (JsPath \ "task").writeNullable[TaskView] and
      (JsPath \ "accessRight").writeNullable[AccessRightView]
    )(unlift(Tasks.unapply))

  implicit val tasksReads: Reads[Tasks] = (
    (JsPath \ "task").readNullable[TaskView] and
      (JsPath \ "accessRight").readNullable[AccessRightView]
    ).tupled.map(Tasks( _ ))
}


case class TaskView(
                    title: Option[Map[String, String]],
                    description: Option[Map[String, String]],
                    deadline: Option[Map[String, Date]],
                    countSupporter: Option[Map[String, Int]]
                   ) extends ViewBase{
  def getValue (fieldname: String, index: Int): Any = {
    fieldname match {
      case "title" => title.get.get(index.toString).get
      case "description" => description.get.get(index.toString).get
      case "deadline" => deadline.get.get(index.toString).get
      case "countSupporter" => countSupporter.get.get(index.toString).get
    }
  }

  def isFieldDefined(fieldname: String, index: Int): Boolean = {
    fieldname match {
      case "title" => {
        title.isDefined match {
          case true => title.get.keySet.contains(index.toString)
          case false => false
        }
      }
      case "description" => {
        description.isDefined match {
          case true => description.get.keySet.contains(index.toString)
          case false => false
        }
      }
      case "deadline" => {
        deadline.isDefined match {
          case true => deadline.get.keySet.contains(index.toString)
          case false => false
        }
      }
      case "countSupporter" => {
        countSupporter.isDefined match {
          case true => countSupporter.get.keySet.contains(index.toString)
          case false => false
        }
      }
    }
  }
}

object TaskView{
  def apply(tuple: (Option[Map[String, String]], Option[Map[String, String]], Option[Map[String, Date]], Option[Map[String, Int]])): TaskView =
    TaskView(tuple._1, tuple._2, tuple._3, tuple._4)
  implicit val dateWrites = new Writes[Date] {
    def writes(date: Date) = JsString(date.toString)
  }

  implicit  val dateReads = new Reads[Date]{
    def reads(json: JsValue): JsResult[Date] = JsSuccess(new SimpleDateFormat("yyyy-MM-dd").parse(json.as[String]))
  }

  implicit val taskViewWrites : OWrites[TaskView] = (
    (JsPath \ "title").writeNullable[Map[String, String]] and
      (JsPath \ "description").writeNullable[Map[String, String]] and
      (JsPath \ "deadline").writeNullable[Map[String, Date]] and
      (JsPath \ "countSupporter").writeNullable[Map[String, Int]]
    )(unlift(TaskView.unapply))

  implicit val taskViewReads : Reads[TaskView] = (
    (JsPath \ "title").readNullable[Map[String, String]].orElse(
      (JsPath \ "title").readNullable[String].map(_.map(p => Map("0" -> p))))and
      (JsPath \ "description").readNullable[Map[String, String]].orElse(
        (JsPath \ "description").readNullable[String].map(_.map(n => Map("0" -> n))))and
      (JsPath \ "deadline").readNullable[Map[String, Date]].orElse(
        (JsPath \ "deadline").readNullable[Date].map(_.map(c => Map("0" -> c)))) and
      (JsPath \ "countSupporter").readNullable[Map[String, Int]].orElse(
        (JsPath \ "countSupporter").readNullable[Int].map(_.map(c => Map("0" -> c))))
    ).tupled.map(TaskView( _ ))
}

case class AccessRightView(
                          uri:  Option[Map[String, String]],
                          name: Option[Map[String, String]],
                          description: Option[Map[String, String]],
                          method: Option[Map[String, String]],
                          service: Option[Map[String, String]]
                          ) extends ViewBase{
  def getValue (fieldname: String, index: Int): Object = {
    fieldname match {
      case "uri" => uri.get.get(index.toString).get
      case "name" => name.get.get(index.toString).get
      case "description" => description.get.get(index.toString).get
      case "method" => method.get.get(index.toString).get
      case "service" => method.get.get(index.toString).get
    }
  }

  def isFieldDefined(fieldname: String, index: Int): Boolean = {
    fieldname match {
      case "uri" => {
        uri.isDefined match {
          case true => uri.get.keySet.contains(index.toString)
          case false => false
        }
      }
      case "name" => {
        name.isDefined match {
          case true => name.get.keySet.contains(index.toString)
          case false => false
        }
      }
      case "description" => {
        description.isDefined match {
          case true => description.get.keySet.contains(index.toString)
          case false => false
        }
      }
      case "method" => {
        method.isDefined match {
          case true => method.get.keySet.contains(index.toString)
          case false => false
        }
      }
      case "service" => {
        service.isDefined match {
          case true => service.get.keySet.contains(index.toString)
          case false => false
        }
      }
    }
  }
}

object AccessRightView{
  def apply(tuple: (Option[Map[String, String]], Option[Map[String, String]], Option[Map[String, String]], Option[Map[String, String]], Option[Map[String, String]])): AccessRightView =
    AccessRightView(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5)

  implicit val accessRightViewWrites : OWrites[AccessRightView] = (
    (JsPath \ "uri").writeNullable[Map[String, String]] and
      (JsPath \ "name").writeNullable[Map[String, String]] and
      (JsPath \ "description").writeNullable[Map[String, String]] and
      (JsPath \ "method").writeNullable[Map[String, String]] and
      (JsPath \ "service").writeNullable[Map[String, String]]
    )(unlift(AccessRightView.unapply))

  implicit val accessRightViewReads : Reads[AccessRightView] = (
    (JsPath \ "uri").readNullable[Map[String, String]].orElse(
      (JsPath \ "uri").readNullable[String].map(_.map(p => Map("0" -> p))))and
      (JsPath \ "name").readNullable[Map[String, String]].orElse(
        (JsPath \ "name").readNullable[String].map(_.map(n => Map("0" -> n))))and
      (JsPath \ "description").readNullable[Map[String, String]].orElse(
        (JsPath \ "description").readNullable[String].map(_.map(c => Map("0" -> c)))) and
      (JsPath \ "method").readNullable[Map[String, String]].orElse(
        (JsPath \ "method").readNullable[String].map(_.map(c => Map("0" -> c)))) and
      (JsPath \ "service").readNullable[Map[String, String]].orElse(
        (JsPath \ "service").readNullable[String].map(_.map(c => Map("0" -> c))))
    ).tupled.map(AccessRightView( _ ))
}