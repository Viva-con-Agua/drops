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

sealed trait Combinations { def step: QueryAST }

/**
  * AND
  * @param step1
  * @param step2
  */
case class A(step1: QueryAST, step2: QueryAST) extends QueryAST {
  override def toSqlStatement = {
    Converter.concat(step1.toSqlStatement, Converter.concat(sql""" AND """, step2.toSqlStatement))
  }
}

/**
  * OR
  * @param step1
  * @param step2
  */
case class O(step1: QueryAST, step2: QueryAST) extends QueryAST {
  override def toSqlStatement ={
    Converter.concat(step1.toSqlStatement, Converter.concat(sql""" OR """, step2.toSqlStatement))
  }
}

case class Combination(f1: QueryAST, f2: List[QueryParser.~[QueryToken, QueryAST]]) extends QueryAST {
  override def toSqlStatement = {
    f2.foldLeft[SQLActionBuilder](f1.toSqlStatement)((res, o) => {
      o._1 match {
        case AND => Converter.concat(res, Converter.concat(sql""" AND """, o._2.toSqlStatement))
        case OR => Converter.concat(res, Converter.concat(sql""" OR """, o._2.toSqlStatement))
        case _ => throw new Exception
      }
    })
  }
}

case class Group(f1: QueryToken, f2: Combination, f3: QueryToken)extends QueryAST{
  override def toSqlStatement = {
    Converter.concat(sql"""(""", Converter.concat(f2.toSqlStatement, sql""")"""))
  }
}

//ToDo: Support  interleaved brackets
case class CombinedGroup(f1: QueryAST, f2: List[QueryParser.~[QueryToken, QueryAST]]) extends QueryAST{
  override def toSqlStatement = {
    f2.foldLeft[SQLActionBuilder](f1.toSqlStatement)((res, o) => {
      o._1 match {
        case AND => Converter.concat(res, Converter.concat(sql""" AND """, o._2.toSqlStatement))
        case OR => Converter.concat(res, Converter.concat(sql""" OR """, o._2.toSqlStatement))
        case _ => throw new Exception
      }
    })
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
    if (filter.keys.contains(entity))
      if(filter.\(entity).get.asInstanceOf[JsObject].keys.contains(field))
        return true

    return false
  }
}