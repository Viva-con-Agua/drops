package utils.Query

import play.api.Logger
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

/**
  * AND
  * @param step1
  * @param step2
  */
case class A(step1: QueryAST, step2: QueryAST) extends QueryAST {
  override def toSqlStatement = {
    val sql1 = step1.toSqlStatement
    val sql2 = step2.toSqlStatement

    Converter.concat(sql1, Converter.concat(sql""" AND """, sql2))
  }
}

/**
  * OR
  * @param step1
  * @param step2
  */
case class O(step1: QueryAST, step2: QueryAST) extends QueryAST {
  override def toSqlStatement ={

    val sql1 = step1.toSqlStatement
    val sql2 = step2.toSqlStatement

    Converter.concat(sql1, Converter.concat(sql""" OR """, sql2))
  }
}

case class Combination(f1: QueryAST, f2: List[QueryParser.~[QueryToken, QueryAST]]) extends QueryAST {
  override def toSqlStatement = {
    var sql = f1.toSqlStatement
    f2.foreach(o => {
      sql = o._1 match{
        case AND => Converter.concat(sql, Converter.concat(sql""" AND """, o._2.toSqlStatement))
        case OR => Converter.concat(sql, Converter.concat(sql""" OR """, o._2.toSqlStatement))
        case _ => throw new Exception
      }
    })
    sql
  }
}

case class Group(f1: QueryToken, f2: Combination, f3: QueryToken)extends QueryAST{
  override def toSqlStatement = {
    val sql = f2.toSqlStatement

    Converter.concat(sql"""(""", Converter.concat(sql, sql""")"""))
  }
}

case class CombinedGroup(f1: QueryAST, f2: List[QueryParser.~[QueryToken, QueryAST]]) extends QueryAST{
  override def toSqlStatement = {
    var sql = f1.toSqlStatement
    f2.foreach(o => {
      sql = o._1 match {
        case AND => Converter.concat(sql, Converter.concat(sql""" AND """, o._2.toSqlStatement))
        case OR => Converter.concat(sql, Converter.concat(sql""" OR """, o._2.toSqlStatement))
        case _ => throw new Exception
      }
    })
    sql
  }
}

sealed trait Conditions extends QueryAST
case class EQ(entity: IDENTIFIER, field: IDENTIFIER, value: String) extends Conditions{
  def toSqlStatement = {
    val fieldname = entity.str + "_" + field.str
    Converter.concat(sql"""#$fieldname""", Converter.concat(sql"""=""", sql"""'#$value'"""))
  }
}
case class LT(entity: IDENTIFIER, field: IDENTIFIER, value: String) extends Conditions {
  def toSqlStatement = {
    val fieldname = entity.str + "_" + field.str
    Converter.concat(sql"""#$fieldname""", Converter.concat(sql"""<""", sql"""'#$value'"""))
  }
}
case class LE(entity: IDENTIFIER, field: IDENTIFIER, value: String) extends Conditions {
  def toSqlStatement = {
    val fieldname = entity.str + "_" + field.str
    Converter.concat(sql"""#$fieldname""", Converter.concat(sql"""<=""", sql"""'#$value'"""))
  }
}

case class GT(entity: IDENTIFIER, field: IDENTIFIER, value: String) extends Conditions {
  def toSqlStatement = {
    val fieldname = entity.str + "_" + field.str
    Converter.concat(sql"""#$fieldname""", Converter.concat(sql""">""", sql"""'#$value'"""))
  }
}
case class GE(entity: IDENTIFIER, field: IDENTIFIER, value: String) extends Conditions {
  def toSqlStatement = {
    val fieldname = entity.str + "_" + field.str
    Converter.concat(sql"""#$fieldname""", Converter.concat(sql""">=""", sql"""'#$value'"""))
  }
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