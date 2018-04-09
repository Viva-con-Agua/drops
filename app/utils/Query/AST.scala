package utils.Query

import play.api.libs.json.JsObject
import slick.jdbc.{PositionedParameters, SQLActionBuilder, SetParameter}
import slick.driver.MySQLDriver.api._
import slick.profile.SqlAction

import scala.util.parsing.input.Positional

//Definition of the abstract syntax tree for querys
sealed trait QueryAST extends Positional {
  //ToDo: function should return a SQLActionBuilder instance (SQL Injection)
  //def toSqlStatement :String
  def toSqlStatement : SQLActionBuilder
}
case class AndThen(step1: QueryAST, step2: QueryAST) extends QueryAST {
  override def toSqlStatement = {
    //step1.toSqlStatement + " AND " + step2.toSqlStatement
    val sql1 = step1.toSqlStatement
    val sql2 = step2.toSqlStatement

    Converter.concat(sql1, Converter.concat(sql"""AND""", sql2))
  }
}

sealed trait Combinations { def step: QueryAST }
case class A(step1: QueryAST, step2: QueryAST) extends QueryAST {
  override def toSqlStatement = {
    //step1.toSqlStatement + " AND " + step2.toSqlStatement
    val sql1 = step1.toSqlStatement
    val sql2 = step2.toSqlStatement

    sql"""#$sql1 AND #$sql2"""
    Converter.concat(sql1, Converter.concat(sql""" AND """, sql2))
  }
} //AND
case class O(step1: QueryAST, step2: QueryAST) extends QueryAST {
  override def toSqlStatement = ???
} //OR

sealed trait Conditions extends QueryAST
case class EQ(entity: IDENTIFIER, field: IDENTIFIER, value: String) extends Conditions{
  //entity.str + "_" + field.str + " = \""+ value + "\""
  def toSqlStatement = {
    val fieldname = entity.str + "_" + field.str
    Converter.concat(sql"""#$fieldname""", Converter.concat(sql"""=""", sql"""'#$value'"""))
  }
}
case class LT(entity: IDENTIFIER, field: IDENTIFIER) extends Conditions {
  override def toSqlStatement = ???
}
case class LE(entity: IDENTIFIER, field: IDENTIFIER) extends Conditions {
  override def toSqlStatement = ???
}
case class LIKE(entity: IDENTIFIER, field: IDENTIFIER, value: String) extends Conditions {
  override def toSqlStatement = {
    val fieldname = entity.str + "_" + field.str
    Converter.concat(sql"""#$fieldname""", Converter.concat(sql""" LIKE """, sql"""'#$value'"""))
  }
}




//ToDo: Extract JSON Object to Case Class for further steps in the filter query pipeline
object QueryAST{
  /**
    * This function validates if there is a value for each term in the filter query.
    * @param step
    * @param filter
    * @return
    */
  def validateStep(step: QueryAST, filter: JsObject) : Boolean = {
    val entity : String = step.asInstanceOf[utils.Query.EQ].entity.str
    val field : String = step.asInstanceOf[utils.Query.EQ].field.str
    //ToDo: Alternate solution. Try Catch is necessary
    // val value : String = filter.\(entity).\(field).as[String]
    if (filter.keys.contains(entity))
      if(filter.\(entity).get.asInstanceOf[JsObject].keys.contains(field))
        return true

    return false
  }
}