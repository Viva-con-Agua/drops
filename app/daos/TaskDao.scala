package daos

import java.sql.{JDBCType, ResultSet}
import java.util.Date
import java.sql.Types
import java.util

import scala.concurrent.Future

import play.api.Play.current
import play.api.libs.json._
import play.api.db._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import models._
import models.Task._
import utils.JsonUtils

import scala.collection.mutable.ListBuffer

/**
  * Created bei jottmann on 26.07.2017
  */

trait TaskDao{
  def getAll(): Future[JsValue]
  def getAllAsObject(): List[Task]
  def create(task:Task): Future[JsValue]
  def create(title: String, description: Option[String], deadline: Option[Date], count_supporter: Option[Int]): Future[JsValue]
  def find(id:Int): Future[JsValue]
}

class MariadbTaskDao extends TaskDao {
  val conn = DB.getConnection()
  val stmt = conn.createStatement

  def getAll(): Future[JsValue] = {
    val response = stmt.executeQuery("SELECT * FROM Task")
    Future { JsonUtils.sqlResultSetToJson(response) }
  }

  def getAllAsObject(): List[Task] = {
    val response : ResultSet = stmt.executeQuery("SELECT * FROM Task")
    sqlResultToList(response)
  }


  def find(id:Int): Future[JsValue] = {
    val query:String = "SELECT * FROM Task WHERE id = ?"
    val preparedStatement = conn.prepareStatement(query)
    preparedStatement.setInt(1, id)
    Future {JsonUtils.sqlResultSetToJson(preparedStatement.executeQuery())}
  }

  def create(task:Task): Future[JsValue] = {

    val query:String = """INSERT INTO Task (title, description, deadline, count_supporter)
        VALUES (?,?,?,?)"""
    val preparedStatement = conn.prepareStatement(query, Array[String]("id"))

    preparedStatement.setString(1, task.title)

    if(task.description != None){
      val description:String = task.description.get
      preparedStatement.setString(2, description)
    }else{
      preparedStatement.setNull(2, Types.VARCHAR)
    }

    if(task.deadline != None){
      val deadline:java.sql.Date = new java.sql.Date(task.deadline.get.getTime)
      preparedStatement.setDate(3,deadline)
    }else{
      preparedStatement.setNull(3, Types.DATE)
    }

    if(task.count_supporter != None){
      val count_supporter:Int = task.count_supporter.get
      preparedStatement.setInt(4, count_supporter)
    }else{
      preparedStatement.setNull(4, Types.INTEGER)
    }

    preparedStatement.execute()
    val rs = preparedStatement.getGeneratedKeys
    rs.next
    val key:Int = rs.getInt(1)

    find(key)
  }

  def create(title: String, description: Option[String], deadline: Option[Date], count_supporter: Option[Int]): Future[JsValue] = {
    val taskModel = Task.apply(title, description, deadline, count_supporter)
    create(taskModel)
  }

  def sqlResultToList(rs: ResultSet) : List[Task] = {
    val tasks : ListBuffer[Task] = ListBuffer()
    val rsmd = rs.getMetaData
    val columnCount = rsmd.getColumnCount

    while (rs.next) {
      val t: Task= new Task(rs.getString(2), Some(rs.getString(3)), Some(rs.getDate(4)), Some(rs.getInt(5)))
      tasks += t

    }
    tasks.toList
  }
}