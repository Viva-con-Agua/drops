package daos.schema

import java.sql.Timestamp
import java.util.Date

import slick.driver.MySQLDriver.api._

import models.database.TaskDB

class TaskTableDef(tag: Tag) extends Table[TaskDB](tag, "Task") {
  def id = column[Long]("id", O.PrimaryKey,O.AutoInc)
  def title = column[String]("title")
  def description= column[String]("description")
  def deadline = column[Date]("deadline")
  def count_supporter = column[Int]("count_supporter")

  implicit val dateColumnType =
    MappedColumnType .base[Date, Timestamp] (
      d => new Timestamp(d.getTime),
      d => new Date(d.getTime)
    )

  def * =
    (id, title, description.?, deadline.?, count_supporter.?) <>((TaskDB.mapperTo _).tupled, TaskDB.unapply)
}