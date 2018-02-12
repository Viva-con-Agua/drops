package daos.schema

import java.util.UUID
import slick.driver.MySQLDriver.api._


class TaskAccessRightTableDef(tag: Tag) extends Table[(Long, Long)](tag, "Task_AccessRight") {
  def taskId = column[Long]("task_id")
  def accessRightId = column[Long]("access_right_id")

  def * = (taskId, accessRightId)

  def pk = primaryKey("primaryKey", (taskId, accessRightId))

  def tK = foreignKey("taskId", taskId, TableQuery[AccessRightTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)
  def aRK = foreignKey("accessRightId", accessRightId, TableQuery[TaskTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)
}



class UserTaskTableDef(tag: Tag) extends Table[(Long, Long)](tag, "User_Task"){
  def userId = column[Long]("user_id")
  def taskId = column[Long]("task_id")

  def * = (userId, taskId)

  def pk = primaryKey("primaryKey", (userId, taskId))
  def uK = foreignKey("userId", userId, TableQuery[UserTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)
  def tK = foreignKey("taskId", taskId, TableQuery[AccessRightTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)
}


class UserRoleTableDef(tag: Tag) extends Table[(Long, Long)](tag, "User_Role"){
  def userId = column[Long]("user_id")
  def roleId = column[Long]("role_id")

  def * = (userId, roleId)

  def pk = primaryKey("primaryKey", (userId, roleId))
  def uK = foreignKey("userId", userId, TableQuery[UserTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)
  def rK = foreignKey("roleId", roleId, TableQuery[RoleTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)
}