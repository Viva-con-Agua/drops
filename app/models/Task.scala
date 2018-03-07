package models

import java.util.Date

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, _}

case class Task (
                  id: Long,
                  title: String,
                  description: Option[String],
                  deadline: Option[Date],
                  count_supporter: Option[Int]
           )

object Task {

  implicit val taskWrites : OWrites[Task] = (
    (JsPath \ "id").write[Long] and
      (JsPath \ "title").write[String] and
      (JsPath \ "description").writeNullable[String] and
      (JsPath \ "deadline").writeNullable[Date] and
      (JsPath \ "count_supporter").writeNullable[Int]
    )(unlift(Task.unapply))

  implicit val taskReads : Reads[Task] = (
    (JsPath \ "id").readNullable[Long] and
      (JsPath \ "title").read[String] and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "deadline").readNullable[Date] and
      (JsPath \ "count_supporter").readNullable[Int]
    ).tupled.map((task) => if(task._1.isEmpty)
    Task(0, task._2, task._3, task._4, task._5)
  else Task(task._1.get, task._2, task._3, task._4, task._5))
}