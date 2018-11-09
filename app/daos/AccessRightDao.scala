package daos

import java.util.UUID
import javax.inject.Inject

import daos.schema._
import models.AccessRight
import models.database.AccessRightDB

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._

/**
  * Created bei jottmann on 29.08.2017
  */

trait AccessRightDao {
  def all(): Future[Seq[AccessRight]]
  def forTask(taskId: Long): Future[Seq[AccessRight]]
  def forTaskList(taskIdList : Seq[Long]): Future[Seq[AccessRight]]
  def forTaskListAndService(taskIdList : Seq[Long], service :String): Future[Seq[AccessRight]]
  def forUserAndService(userId: UUID, service: String) : Future[Seq[AccessRight]]
  def find(id:Long): Future[Option[AccessRight]]
  def create(accessRight:AccessRight): Future[Option[AccessRight]]
  def delete(id:Long): Future[Int]
}

class MariadbAccessRightDao @Inject()(dbConfigProvider: DatabaseConfigProvider) extends AccessRightDao {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  val accessRights = TableQuery[AccessRightTableDef]
  val taskAccessRights = TableQuery[TaskAccessRightTableDef]
  val tasks = TableQuery[TaskTableDef]
  val userTasks = TableQuery[UserTaskTableDef]
  val users = TableQuery[UserTableDef]


  def all(): Future[Seq[AccessRight]] = dbConfig.db.run(accessRights.result).map(aR => aR.map(accessRight => accessRight.toAccessRight))

  def forTask(taskId: Long) : Future[Seq[AccessRight]] = {
    val action = for {
      (ar, _) <- (accessRights join taskAccessRights.filter(tar => tar.taskId === taskId) on (_.id === _.accessRightId))
    } yield (ar)
    dbConfig.db.run(action.result).map(aR => aR.map(accessRight => accessRight.toAccessRight))
  }

  def find(id: Long): Future[Option[AccessRight]] = dbConfig.db.run(accessRights.filter(ar => ar.id === id).result).map(_.headOption.map(_.toAccessRight))

  def create(accessRight: AccessRight): Future[Option[AccessRight]] = {
    dbConfig.db.run((accessRights returning accessRights.map(_.id)) += AccessRightDB(accessRight)).flatMap((ac)=> find(ac))
  }

  def delete(id: Long): Future[Int] = dbConfig.db.run(accessRights.filter(ar => ar.id === id).delete)

  def forTaskList(taskIdList : Seq[Long]): Future[Seq[AccessRight]] = {
    val action = for {
      (ar, _) <- (accessRights join taskAccessRights.filter(tar => tar.taskId.inSet(taskIdList)) on (_.id === _.accessRightId))
    } yield (ar)
    dbConfig.db.run(action.result).map(aR => aR.map(accessRight => accessRight.toAccessRight))
  }

  def forTaskListAndService(taskIdList : Seq[Long], service :String): Future[Seq[AccessRight]] = {
    val action = for {
      (ar, _) <- (accessRights.filter(aR => aR.service === service) join taskAccessRights.filter(tar => tar.taskId.inSet(taskIdList)) on (_.id === _.accessRightId))
    } yield (ar)
    dbConfig.db.run(action.result).map(aR => aR.map(accessRight => accessRight.toAccessRight))
  }

  def forUserAndService(userId: UUID, service: String): Future[Seq[AccessRight]] = {
    dbConfig.db.run(users.filter(u => u.publicId === userId).result).flatMap(user => {
      val action = for {
        (((ar, _), _), _) <- (accessRights.filter(aR => aR.service === service)
          join taskAccessRights on (_.id === _.accessRightId)
          join tasks on (_._2.taskId === _.id)
          join userTasks.filter(uT => uT.userId === user.head.id) on (_._2.id === _.taskId)
          )

      } yield (ar)

      dbConfig.db.run(action.result).map(aR => aR.map(accessRight => accessRight.toAccessRight))
    })
  }
}
