package daos.schema

import java.util.UUID

import slick.driver.MySQLDriver.api._
import models.database.{ProfileOrganizationDB, SupporterCrewDB}
import slick.lifted.{MappedProjection, ProvenShape}

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

class ProfileOrganizationTableDef(tag: Tag) extends Table[(Long, Long)](tag, "Profile_Organization"){
  def profileId = column[Long]("profile_id")
  def organizationId = column[Long]("organization_id") 
  def role = column[String]("role")

  def * = (profileId, organizationId)
  
  def pk = primaryKey("primaryKey", (profileId, organizationId))
  def rK = foreignKey("profileId", profileId, TableQuery[ProfileTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)
  def oK = foreignKey("organizationId", organizationId, TableQuery[OrganizationTableDef])(_.id, onUpdate = ForeignKeyAction.Cascade)
}

class SupporterCrewTableDef(tag: Tag) extends Table[SupporterCrewDB](tag, "Supporter_Crew"){
  def supporterId = column[Long]("supporter_id")
  def crewId = column[Long]("crew_id")
  def role = column[String]("role")
  def pillar = column[String]("pillar")
  def created = column[Long]("created")
  def updated = column[Long]("updated")

//  def from(sc : SupporterCrewDB): Option[(Long, Long, Option[String], Option[String], Long, Long)] = Some {
//    (sc.supporterId, sc.crewId, sc.role, sc.pillar, sc.created, sc.updated)
//  }
//
  def * =
    (supporterId, crewId, role.?, pillar.?, created, updated) <> (SupporterCrewDB.tupled, SupporterCrewDB.unapply)
//a
//  def * : ProvenShape[SupporterCrewDB] = ProvenShape.proveShapeOf(
//    (supporterId, crewId, role.?, pillar.?, created, updated) <> (SupporterCrewDB.tupled, SupporterCrewDB.unapply)
//  )(MappedProjection.mappedProjectionShape)

//  def * = (supporterId, crewId)

  def pk = primaryKey("primaryKey", (supporterId, crewId))
  def sK = foreignKey("supporterId", supporterId, TableQuery[SupporterTableDef])(_.id)
  def cK = foreignKey("crewId", crewId, TableQuery[CrewTableDef])(_.id)
}
