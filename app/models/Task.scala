package models

import java.util.Date

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}

case class Task(
                 title: String,
                 description: Option[String],
                 deadline: Option[Date],
                 count_supporter: Option[Int]
               )

object Task{
  def apply(tuple: (String, Option[String], Option[Date], Option[Int])): Task =
    Task(tuple._1, tuple._2, tuple._3, tuple._4)

  def apply(title: String, description: String, deadline: Date, count_supporter: Int) : Task =
   Task(title, Some(description), Some(deadline), Some(count_supporter))

  implicit val taskReads : Reads[Task] = (
    (JsPath \ "title").read[String] and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "deadline").readNullable[Date] and
      (JsPath \ "count_supporter").readNullable[Int]
    ).tupled.map(Task( _ ))
}


