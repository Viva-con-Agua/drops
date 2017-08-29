package daos

import java.net.URI
import java.sql.{ResultSet, Types}
import java.util.Date

import scala.concurrent.Future

import play.api.Play.current
import play.api.libs.json._
import play.api.db.DB
import play.api.libs.concurrent.Execution.Implicits.defaultContext



import models.AccessRight
import utils.JsonUtils


/**
  * Created bei jottmann on 29.08.2017
  */

trait AccessRightDao {
  def find(id:Int): Future[JsValue]
  def findByUri(uri: URI): Future[AccessRight]
  def create(accessRight:AccessRight): Future[JsValue]
  def create(uri: URI, name:Option[String], description: Option[String]): Future[JsValue]
}

class MariadbAccessRightDao extends AccessRightDao {
  val conn = DB.getConnection()
  val stmt = conn.createStatement


  def find(id:Int): Future[JsValue] = {
    val query:String = "SELECT * FROM AccessRight WHERE id = ?"
    val preparedStatement = conn.prepareStatement(query)
    preparedStatement.setInt(1, id)
    Future {JsonUtils.sqlResultSetToJson(preparedStatement.executeQuery())}
  }

  def findByUri(uri: URI): Future[AccessRight] = {
    val query:String = "SELECT * FROM AccessRight WHERE uri = ?"
    val preparedStatement = conn.prepareStatement(query)
    preparedStatement.setString(1, uri.toString())
    val rs = preparedStatement.executeQuery()

    //ToDo: Write generic ResultSet to Case Class Converter
    Future {AccessRight(new URI(rs.getObject(2).toString),rs.getObject(3).toString, rs.getObject(4).toString)}
  }

  def create(accessRight: AccessRight): Future[JsValue] = {
    val query:String = """INSERT INTO Task (uri, name, description)
        VALUES (?,?,?)"""
    val preparedStatement = conn.prepareStatement(query, Array[String]("id"))

    preparedStatement.setString(1, accessRight.uri.toString)

    if(accessRight.name != None){
      val name:String = accessRight.name.get
      preparedStatement.setString(2, name)
    }else{
      preparedStatement.setNull(2, Types.VARCHAR)
    }

    if(accessRight.description != None){
      val description:String = accessRight.description.get
      preparedStatement.setString(3, description)
    }else{
      preparedStatement.setNull(3, Types.VARCHAR)
    }

    preparedStatement.execute()
    val rs = preparedStatement.getGeneratedKeys
    rs.next
    val key:Int = rs.getInt(1)

    find(key)
  }

  def create(uri: URI, name: Option[String], description: Option[String]): Future[JsValue] = {
    val accessRightModel = AccessRight.apply(uri, name, description);
    create(accessRightModel)
  }
}
