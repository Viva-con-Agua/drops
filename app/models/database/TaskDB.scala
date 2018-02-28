package models.database

import java.text.SimpleDateFormat
import java.util.Date

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, _}

case class TaskDB(
                 id: Long,
                 title: String,
                 description: Option[String],
                 deadline: Option[Date],
                 count_supporter: Option[Int]
               )

object TaskDB{

  def mapperTo(
                id: Long, title: String,
                description: Option[String], deadline: Option[Date], count_supporter: Option[Int]
              ) = apply(id, title, description, deadline, count_supporter)

  def apply(tuple: (Long, String, Option[String], Option[Date], Option[Int])): TaskDB =
    TaskDB(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5)

  def apply(id: Long, tuple: (String, Option[String], Option[Date], Option[Int])): TaskDB =
    TaskDB(0, tuple._1, tuple._2, tuple._3, tuple._4)

  def apply(id: Long, title: String, description: String, deadline: Date, count_supporter: Int) : TaskDB =
   TaskDB(id, title, Some(description), Some(deadline), Some(count_supporter))

  implicit val dateWrites = new Writes[Date] {
    def writes(date: Date) = JsString(date.toString)
  }

  implicit  val dateReads = new Reads[Date]{
    def reads(json: JsValue): JsResult[Date] = JsSuccess(new SimpleDateFormat("yyyy-MM-dd").parse(json.as[String]))
  }

  implicit val taskWrites : OWrites[TaskDB] = (
    (JsPath \ "id").write[Long] and
      (JsPath \ "title").write[String] and
      (JsPath \ "description").writeNullable[String] and
      (JsPath \ "deadline").writeNullable[Date] and
      (JsPath \ "count_supporter").writeNullable[Int]
    )(unlift(TaskDB.unapply))

  implicit val taskReads : Reads[TaskDB] = (
    (JsPath \ "id").readNullable[Long] and
      (JsPath \ "title").read[String] and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "deadline").readNullable[Date] and
      (JsPath \ "count_supporter").readNullable[Int]
    ).tupled.map((task) => if(task._1.isEmpty)
      TaskDB(0, task._2, task._3, task._4, task._5)
      else TaskDB(task._1.get, task._2, task._3, task._4, task._5))
}


