package daos

import java.sql.Timestamp
import java.util.Date

import models.Task
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created bei jottmann on 26.07.2017
  */

trait TaskDao{
  def all(): Future[Seq[Task]]
  def create(task:Task): Future[Option[Task]]
  def find(id:Long): Future[Option[Task]]
  def delete(id:Long): Future[Int]
}

class TaskTableDef(tag: Tag) extends Table[Task](tag, "Task") {
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
    (id, title, description.?, deadline.?, count_supporter.?) <>((Task.mapperTo _).tupled, Task.unapply)
}

class MariadbTaskDao extends TaskDao {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  val tasks = TableQuery[TaskTableDef]

  def all(): Future[Seq[Task]] = dbConfig.db.run(tasks.result)

  def find(id: Long): Future[Option[Task]] = dbConfig.db.run(tasks.filter(t => t.id === id).result).map(_.headOption)

  def create(task: Task): Future[Option[Task]] = {
    dbConfig.db.run((tasks returning tasks.map(_.id)) += task).flatMap((id) => find(id))
  }

  def delete(id: Long): Future[Int] = dbConfig.db.run(tasks.filter(t => t.id === id).delete)
}
