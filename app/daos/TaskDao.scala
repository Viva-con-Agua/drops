package daos

import java.util.UUID

import daos.schema.{TaskTableDef, UserTableDef, UserTaskTableDef}
import models.Task
import models.database.TaskDB
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created bei jottmann on 26.07.2017
  */

//ToDo: DAOs should always return business models
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
  val users = TableQuery[UserTableDef]

  def all(): Future[Seq[Task]] = dbConfig.db.run(tasks.result).map(t => t.map(task => task.toTask))


  def find(id: Long): Future[Option[Task]] = dbConfig.db.run(tasks.filter(t => t.id === id).result).map(_.headOption.map(_.toTask))

  def create(task: Task): Future[Option[Task]] = {
    dbConfig.db.run((tasks returning tasks.map(_.id)) += TaskDB(task)).flatMap((id) => find(id))
  }

  def delete(id: Long): Future[Int] = dbConfig.db.run(tasks.filter(t => t.id === id).delete)

  def forUser(userId : UUID) : Future[Seq[Task]] = {
    dbConfig.db.run(users.filter(u => u.publicId === userId).result).flatMap(user => {
      val action = for{
        (t, _) <- (tasks join userTasks.filter(ut => ut.userId === user.head.id) on (_.id === _.taskId))
      } yield(t)
      dbConfig.db.run(action.result).map(t => t.map(task => task.toTask))
    })
  }

  def idsForUser(userId : UUID) : Future[Seq[Long]] = {
    dbConfig.db.run(users.filter(u => u.publicId === userId).result).flatMap(user => {
      dbConfig.db.run(userTasks.filter(ut => ut.userId === user.head.id).map(_.taskId).result)
    })
  }
}
