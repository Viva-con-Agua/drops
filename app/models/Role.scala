package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Created by johann on 14.12.16.
  */
trait Role {
  def name : String
}

object Role {

  def apply(role: String): Role = ContextFreeRole(role)

  def apply[T](context: T): Role = context match {
    case (crew: Crew, pillar: Pillar) => VolunteerManager(crew, pillar)
    case _ => apply("Unknown")
  }

  def unapply(role: Role): Option[String] = Some(role.name)

  def getAll : Set[Role] = Set(RoleAdmin, RoleEmployee, RoleSupporter)

  implicit val roleJsonWrites = Writes[Role] {
    case cf: ContextFreeRole => ContextFreeRole.contextFreeRoleJsonFormat.writes(cf)
    case vm: VolunteerManager => VolunteerManager.volunteerManagerWrites.writes(vm)
  }

  implicit val roleJsonReads = Reads[Role] {
    case json: JsObject => (json \ "crew").toOption match {
      case Some( _ ) => VolunteerManager.volunteerManagerReads.reads(json)
      case _ => ContextFreeRole.contextFreeRoleJsonFormat.reads(json)
    }
    case _ => JsError()//ContextFreeRole.contextFreeRoleJsonFormat.reads(Json.toJson[Role](ContextFreeRole( "" )))
  }
}

trait ContextFreeRole extends Role

object ContextFreeRole {

  def apply(role: String): ContextFreeRole = role match {
    case RoleAdmin.name             => RoleAdmin
    case RoleEmployee.name          => RoleEmployee
    case RoleSupporter.name         => RoleSupporter
    case _                          => RoleUnknown
  }

  def unapply(role: ContextFreeRole): Option[String] = Some(role.name)

  implicit val contextFreeRoleJsonFormat = Json.format[ContextFreeRole]
}

/**
  * Administration role
  */
object RoleAdmin extends ContextFreeRole {
  val name = "admin"
}

/**
  * Administration role
  */
object RoleEmployee extends ContextFreeRole {
  val name = "employee"
}

/**
  * Normal user role
  */
object RoleSupporter extends ContextFreeRole {
  val name = "supporter"
}

/**
  * The generic unknown role
  */
object RoleUnknown extends ContextFreeRole {
  val name = "-"
}

trait Context[T] {
  def context: T
}

case class VolunteerManager(name: String, context: (Crew, Pillar)) extends Role with Context[(Crew, Pillar)] {
  override def equals(o: scala.Any): Boolean = o match {
    case vm : VolunteerManager =>
      this.name == vm.name && this.context._1 == vm.context._1 && this.context._2 == vm.context._2
    case _ => false
  }

  def forCrew(other: Crew): Boolean = this.context._1 == other
  def forCrew(crewName: String): Boolean = this.context._1.name == other
  def isResponsibleFor(pillar: Pillar): Boolean = this.context._2 == pillar
}

object VolunteerManager {

  def apply(name: String, crew: Crew, pillar: Pillar): VolunteerManager = VolunteerManager(name, (crew, pillar))

  def apply(crew: Crew, pillar: Pillar): VolunteerManager = VolunteerManager("VolunteerManager", (crew, pillar))

  def apply(tupled: (String, Crew, Pillar)): VolunteerManager = VolunteerManager(tupled._1, tupled._2, tupled._3)

  implicit val volunteerManagerWrites: Writes[VolunteerManager] = (
    (JsPath \ "name").write[String] and
    (JsPath \ "crew").write[Crew] and
    (JsPath \ "pillar").write[Pillar]
  )(vm => (vm.name, vm.context._1, vm.context._2))

  implicit val volunteerManagerReads: Reads[VolunteerManager] = (
    (JsPath \ "name").read[String] and
      (JsPath \ "crew").read[Crew] and
      (JsPath \ "pillar").read[Pillar]
    )((name, crew, pillar) => VolunteerManager.apply(name, crew, pillar))
}