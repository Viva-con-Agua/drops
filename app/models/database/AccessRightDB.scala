package models.database

import java.net.URI

import models.{AccessRight, HttpMethod}
import models.HttpMethod.HttpMethod

/**
  * Created bei jottmann on 29.08.2017
  */


/**
  * Definition of the database access right model
  * @param id
  * @param uri
  * @param method
  * @param name
  * @param description
  * @param service
  */
case class AccessRightDB (
                         id: Long,
                         uri: URI,
                         method: String,
                         name: Option[String],
                         description: Option[String],
                         service: String
                      ){
  def toAccessRight : AccessRight = {
    val http_method : HttpMethod = method match{
      case "POST" => HttpMethod.POST
      case "GET" => HttpMethod.GET
    }
    AccessRight(id, uri, http_method, name, description, service)
  }
}
object AccessRightDB extends ((Long, URI, String, Option[String], Option[String], String) => AccessRightDB ){
  def apply(tuple: (Long, URI, String, Option[String], Option[String], String)): AccessRightDB =
    AccessRightDB(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6)

  def apply(accessRight : AccessRight) : AccessRightDB =
    AccessRightDB(accessRight.id, accessRight.uri, accessRight.method.toString, accessRight.name, accessRight.description, accessRight.service)

}

