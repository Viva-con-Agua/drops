package models.database

import models.{ContextFreeRole, Role, VolunteerManager}

case class SupporterCrewDB (
  supporterId: Long,
  crewId: Long,
  role: Option[String],
  pillar: Option[String],
  created: Long,
  updated: Long
)

object SupporterCrewDB extends ((Long, Long, Option[String], Option[String], Long, Long) => SupporterCrewDB ) {

  def apply(
             supporterId: Long,
             crewId: Long,
             role: Option[Role]
           ): SupporterCrewDB = role match {
    case Some(vm: VolunteerManager) => SupporterCrewDB(supporterId, crewId, Some(vm.name), Some(vm.context._2.name), System.currentTimeMillis(), System.currentTimeMillis())
    case Some(cf: ContextFreeRole) => SupporterCrewDB(supporterId, crewId, Some(cf.name), None, System.currentTimeMillis(), System.currentTimeMillis())
    case _ => SupporterCrewDB(supporterId, crewId, None, None, System.currentTimeMillis(), System.currentTimeMillis())
  }

  def * (
             supporterId: Long,
             crewId: Long,
             roles: Set[Role]
           ): Set[SupporterCrewDB] =
    Set(SupporterCrewDB(supporterId, crewId, roles.headOption)) ++
      roles.tail.map(role => SupporterCrewDB(supporterId, crewId, Some(role)))

  def mapperTo(supporterId: Long, crewId: Long, role: Option[String], pillar: Option[String], created: Long, updated: Long) =
    apply(supporterId, crewId, role, pillar, created, updated)

}

