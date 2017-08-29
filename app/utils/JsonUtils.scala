package utils

import java.sql.ResultSet
import java.util.Date
import play.api.libs.json._

object JsonUtils {
  //TODO: create your own implementation
  //ToDo: JsObject
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
