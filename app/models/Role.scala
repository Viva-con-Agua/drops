package models

import play.api.libs.json.Json

/**
  * Created by johann on 14.12.16.
  */
trait Role {
  def name : String
}

object Role {

  def apply(role: String): Role = role match {
    case RoleAdmin.name             => RoleAdmin
    case RoleEmployee.name          => RoleEmployee
    case RoleVolunteerManager.name  => RoleVolunteerManager
    case RoleSupporter.name         => RoleSupporter
    case _                          => RoleUnknown
  }

  def unapply(role: Role): Option[String] = Some(role.name)

  implicit val roleJsonFormat = Json.format[Role]
}

/**
  * Administration role
  */
object RoleAdmin extends Role {
  val name = "admin"
}

/**
  * Administration role
  */
object RoleEmployee extends Role {
  val name = "employee"
}

/**
  * Administration role
  */
object RoleVolunteerManager extends Role {
  val name = "volunteerManager"
}

/**
  * Normal user role
  */
object RoleSupporter extends Role {
  val name = "supporter"
}

/**
  * The generic unknown role
  */
object RoleUnknown extends Role {
  val name = "-"
}