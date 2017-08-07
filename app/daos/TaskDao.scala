package daos

import java.sql.{JDBCType, ResultSet}
import java.util.Date
import java.sql.Types
import java.util

import play.api.Play.current
import play.api.libs.json._
import play.api.db._
import models._
import models.Task._

import scala.collection.mutable.ListBuffer

/**
  * Created bei jottmann on 26.07.2017
  */

trait TaskDao{
  def getAll(): JsValue
  def getAllAsObject(): List[Task]
  def create(task:Task): JsValue
  def create(title: String, description: Option[String], deadline: Option[Date], count_supporter: Option[Int]): JsValue
  def find(id:Int): JsValue
}

class MariadbTaskDao extends TaskDao {
  val conn = DB.getConnection()
  val stmt = conn.createStatement

  //todo how to handle this with future
  //def find(taskId:UUID): ResultSet = //Option[Task] =
  def getAll(): JsValue = {
    val response = stmt.executeQuery("SELECT * FROM Task")
    sqlResultSetToJson(response)
  }

  def getAllAsObject(): List[Task] = {
    val response : ResultSet = stmt.executeQuery("SELECT * FROM Task")
    sqlResultToList(response)
  }


  def find(id:Int): JsValue = {
    val query:String = "SELECT * FROM Task WHERE id = ?"
    val preparedStatement = conn.prepareStatement(query)
    preparedStatement.setInt(1, id)
    sqlResultSetToJson(preparedStatement.executeQuery())
  }
  def create(task:Task): JsValue = {

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

  def create(title: String, description: Option[String], deadline: Option[Date], count_supporter: Option[Int]): JsValue = {
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


  //TODO: create your own implementation
  //Source: https://stackoverflow.com/questions/23185334/how-to-send-results-of-a-scala-staticquery-select-query-direct-to-json-object
  def sqlResultSetToJson(rs: ResultSet): JsValue = {
    // This is loosely ported from https://gist.github.com/kdonald/2137988

    val rsmd = rs.getMetaData
    val columnCount = rsmd.getColumnCount

    var qJsonArray: JsArray = Json.arr()
    while (rs.next) {
      var index = 1

      var rsJson: JsObject = Json.obj()
      while (index <= columnCount) {
        val column = rsmd.getColumnLabel(index)
        val columnLabel = column.toLowerCase()

        val value = rs.getObject(column)
        if (value == null) {
          rsJson = rsJson ++ Json.obj(
            columnLabel -> JsNull
          )
        } else if (value.isInstanceOf[Integer]) {
          println(value.asInstanceOf[Integer])
          rsJson = rsJson ++ Json.obj(
            columnLabel -> value.asInstanceOf[Int]
          )
        } else if (value.isInstanceOf[String]) {
          println(value.asInstanceOf[String])
          rsJson = rsJson ++ Json.obj(
            columnLabel -> value.asInstanceOf[String]
          )
        } else if (value.isInstanceOf[Boolean]) {
          rsJson = rsJson ++ Json.obj(
            columnLabel -> value.asInstanceOf[Boolean]
          )
        } else if (value.isInstanceOf[Date]) {
          rsJson = rsJson ++ Json.obj(
            columnLabel -> value.asInstanceOf[Date].getTime
          )
        } else if (value.isInstanceOf[Long]) {
          rsJson = rsJson ++ Json.obj(
            columnLabel -> value.asInstanceOf[Long]
          )
        } else if (value.isInstanceOf[Double]) {
          rsJson = rsJson ++ Json.obj(
            columnLabel -> value.asInstanceOf[Double]
          )
        } else if (value.isInstanceOf[Float]) {
          rsJson = rsJson ++ Json.obj(
            columnLabel -> value.asInstanceOf[Float]
          )
        } else if (value.isInstanceOf[BigDecimal]) {
          rsJson = rsJson ++ Json.obj(
            columnLabel -> value.asInstanceOf[BigDecimal]
          )
        } else {
          throw new IllegalArgumentException("Unmappable object type: " + value.getClass)
        }
        index += 1
      }
      qJsonArray = qJsonArray :+ rsJson
    }
    qJsonArray
  }

}
