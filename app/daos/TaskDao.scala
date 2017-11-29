package daos

import java.util.UUID

import daos.schema.{TaskTableDef, UserTaskTableDef}
import models.database.Task
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
  def forUser(userId: UUID): Future[Seq[Task]]
  def idsForUser(userId : UUID) : Future[Seq[Long]]
}



class MariadbTaskDao extends TaskDao {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  val tasks = TableQuery[TaskTableDef]
  val userTasks = TableQuery[UserTaskTableDef]

  def all(): Future[Seq[Task]] = dbConfig.db.run(tasks.result)

  def find(id: Long): Future[Option[Task]] = dbConfig.db.run(tasks.filter(t => t.id === id).result).map(_.headOption)

  def create(task: Task): Future[Option[Task]] = {
    dbConfig.db.run((tasks returning tasks.map(_.id)) += task).flatMap((id) => find(id))
  }

  def delete(id: Long): Future[Int] = dbConfig.db.run(tasks.filter(t => t.id === id).delete)

  def forUser(userId : UUID) : Future[Seq[Task]] = {
    val action = for{
      (t, _) <- (tasks join userTasks.filter(ut => ut.userId === userId) on (_.id === _.taskId))
    } yield(t)
    dbConfig.db.run(action.result)
  }

  def idsForUser(userId : UUID) : Future[Seq[Long]] = {
    dbConfig.db.run(userTasks.filter(ut => ut.userId === userId).map(_.taskId).result)
  }
}
