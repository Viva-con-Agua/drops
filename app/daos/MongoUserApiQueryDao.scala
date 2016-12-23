package daos

import api.{ApiQuery, Group, PillarArea, RoleArea}
import models.{Pillar, Role, User}
import play.api.libs.json._


trait UserApiQueryDao[A] {
  def filter : A
  def filterByGroups(groups: Set[Group]) : A
  def filterBySearch(keyword: String, fields: Set[String]): A
}

case class MongoUserApiQueryDao(query: ApiQuery) extends UserApiQueryDao[JsObject] {

  def filter = (query.filterBy.groups match {
    case Some(groups) => filterByGroups(groups)
    case _ => Json.obj()
  }) ++ (query.filterBy.search match {
    case Some(search) => filterBySearch(search.keyword, search.fields)
    case None => Json.obj()
  })

  def filterByGroups(groups: Set[Group]) = groups.foldLeft(Json.obj())((json, group) => json ++ filterByGroup(group))

  override def filterBySearch(keyword: String, fields: Set[String]): JsObject = Json.obj("$or" ->
    fields.foldLeft(Json.arr())((conditions, field) =>
      conditions :+ Json.obj(field -> Json.obj("$regex" -> (".*" + keyword + ".*")))
    )
  )

  private def filterByGroup(group: Group) = group.area match {
    case RoleArea => Json.obj(
      "roles.role" -> group.groupName
    )
    case PillarArea => Json.obj(
      "profiles.supporter.pillars.pillar" -> group.groupName
    )
  }
}
