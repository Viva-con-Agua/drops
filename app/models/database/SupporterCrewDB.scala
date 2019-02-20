package models.database

import models._


case class SupporterCrewDB (
  id: Long,
  supporterId: Long,
  crewId: Long,
  role: Option[String],
  pillar: Option[String],
  created: Long,
  updated: Long,
  active: Option[String],
  nvmDate: Option[Long]
) {
  def toRole(crew: Crew) : Option[Role] =
    // ignoring the fact, that `role` could contain something different than "VolunteerManager"
    role.flatMap(_ => pillar.map(p => Role[(Crew, Pillar)]((crew, Pillar(p)))))
  
  // convert SupporterCrewDB in nvmDate for SupporterCrewDB.read 
 // def nvm: Option[Long] = 

}

object SupporterCrewDB extends ((Long, Long, Long, Option[String], Option[String], Long, Long, Option[String], Option[Long]) => SupporterCrewDB ) {

  def apply(
             supporterId: Long,
             crewId: Long,
             role: Option[Role],
             supporterCrewDBID: Option[Long],
             active: Option[String],
             nvmDate: Option[Long]
           ): SupporterCrewDB = role match {
    case Some(vm: VolunteerManager) => SupporterCrewDB(supporterCrewDBID.getOrElse(0), supporterId, crewId, Some(vm.name), Some(vm.context._2.name), System.currentTimeMillis(), System.currentTimeMillis(), active, nvmDate)
    case Some(cf: ContextFreeRole) => SupporterCrewDB(supporterCrewDBID.getOrElse(0), supporterId, crewId, Some(cf.name), None, System.currentTimeMillis(), System.currentTimeMillis(), active, nvmDate)
    case _ => SupporterCrewDB(supporterCrewDBID.getOrElse(0), supporterId, crewId, None, None, System.currentTimeMillis(), System.currentTimeMillis(), active, nvmDate)
  }

  def * (
             supporterId: Long,
             crewId: Long,
             roles: Set[Role],
             supporterCrewDBID : Option[Long],
             active: Option[String],
             nvmDate: Option[Long]
           ): Set[SupporterCrewDB] = {
    roles.size match {
      case i if i == 1 => Set(SupporterCrewDB(supporterId, crewId, roles.headOption, supporterCrewDBID, active, nvmDate))
      case i if i > 1 => roles.map(role => SupporterCrewDB(supporterId, crewId, Some(role), None, active, nvmDate))
      case _ => Set(SupporterCrewDB(supporterId, crewId, None, supporterCrewDBID, active, nvmDate))
    }
  }

  def mapperTo(id: Long, supporterId: Long, crewId: Long, role: Option[String], pillar: Option[String], created: Long, updated: Long, active: Option[String], nvmDate: Option[Long]) =
    apply(id, supporterId, crewId, role, pillar, created, updated, active, nvmDate)

  def read(entries: Seq[(Option[(SupporterCrewDB ,Crew)])]):  Option[(Crew, Seq[Role])] = {
    // to Map[SupporterDB -> Seq[Option[(Option[Role], Crew)]]
      entries.map(orc => orc.isDefined match {
        case true => orc.map(rc => 
            //rc is a Seq entry. rc._1 = SupporterCrewDB and call the toRole function wir Crew rc._2.
            //the tuple is a (Option[Role], Crew)
            (rc._1.toRole(rc._2), rc._2))
            //filter the seq[(Option[Role], Crew)] by Role is defined
            .filter(_._1.isDefined)
            //map the result to Seq[Role, Crew] and return
            .map(rc => (rc._1.get, rc._2))
        case false => None 
      }).filter(_.isDefined) // filter Option[Seq[Role, Crew]] by isDefined
        // get Seq[Role, Crew]
        .map(rc => rc.get)
        // groupBy Crew
        .groupBy(_._2)
        // to Seq Seq[(Crew, Seq[(Role, Crew)])]
        .toSeq
        .map(result => 
            // map to Seq[(Crew, Seq[Role])]
            (result._1, result._2.map(entry => 
                (entry._1)))
            // return Option[(Crew, Seq[Role])]
            ).headOption
          

  }

}

