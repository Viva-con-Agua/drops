package models.database

import models._

case class SupporterCrewDB (
  id: Long,
  supporterId: Long,
  crewId: Long,
  role: Option[String],
  pillar: Option[String],
  created: Long,
  updated: Long
) {
  def toRole(crew: Crew) : Option[Role] =
    // ignoring the fact, that `role` could contain something different than "VolunteerManager"
    role.flatMap(_ => pillar.map(p => Role[(Crew, Pillar)]((crew, Pillar(p)))))
}

object SupporterCrewDB extends ((Long, Long, Long, Option[String], Option[String], Long, Long) => SupporterCrewDB ) {

  def apply(
             supporterId: Long,
             crewId: Long,
             role: Option[Role],
             supporterCrewDBID : Option[Long]
           ): SupporterCrewDB = role match {
    case Some(vm: VolunteerManager) => SupporterCrewDB(supporterCrewDBID.getOrElse(0), supporterId, crewId, Some(vm.name), Some(vm.context._2.name), System.currentTimeMillis(), System.currentTimeMillis())
    case Some(cf: ContextFreeRole) => SupporterCrewDB(supporterCrewDBID.getOrElse(0), supporterId, crewId, Some(cf.name), None, System.currentTimeMillis(), System.currentTimeMillis())
    case _ => SupporterCrewDB(supporterCrewDBID.getOrElse(0), supporterId, crewId, None, None, System.currentTimeMillis(), System.currentTimeMillis())
  }

  def * (
             supporterId: Long,
             crewId: Long,
             roles: Set[Role],
             supporterCrewDBID : Option[Long]
           ): Set[SupporterCrewDB] = {
    roles.size match {
      case i if i == 1 => Set(SupporterCrewDB(supporterId, crewId, roles.headOption, supporterCrewDBID))
      case i if i > 1 => roles.map(role => SupporterCrewDB(supporterId, crewId, Some(role), None))
      case _ => Set(SupporterCrewDB(supporterId, crewId, None, supporterCrewDBID))
    }
  }

  def mapperTo(id: Long, supporterId: Long, crewId: Long, role: Option[String], pillar: Option[String], created: Long, updated: Long) =
    apply(id, supporterId, crewId, role, pillar, created, updated)

  def read(entries: Seq[(Option[(SupporterCrewDB ,Crew)])]):  Option[(Crew, Seq[Role])] = {
    // to Map[SupporterDB -> Seq[Option[(Option[Role], Crew)]]
      entries.map(orc => orc.isDefined match {
        case true => orc.map(rc => 
            //rc is a Seq entry. And then magic mapping ...
            (rc._1.toRole(rc._2), rc._2))
            .filter(_._1.isDefined)
            .map(rc => (rc._1.get, rc._2))
        case false => None
      }).filter(_.isDefined)
        .map(rc => rc.get)
        .groupBy(_._2)
        .toSeq
        .map(result => 
            (result._1, result._2.map(entry => 
                (entry._1)))
            ).headOption
          

  }

}

