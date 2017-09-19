package daos

import slick.driver.MySQLDriver.api._

class TaskAccessRightTableDef(tag: Tag) extends Table[(Long, Long)](tag, "Task_AccessRight") {
  def taskId = column[Long]("task_id")
  def accessRightId = column[Long]("access_right_id")

  def * = (taskId, accessRightId)

  def pk = primaryKey("primaryKey", (taskId, accessRightId))

  def tK = foreignKey("taskId", taskId, TableQuery[AccessRightTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)
  def aRK = foreignKey("accessRightId", accessRightId, TableQuery[TaskTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)
}
