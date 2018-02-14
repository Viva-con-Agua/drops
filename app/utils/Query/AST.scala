package utils.Query

import play.api.libs.json.JsObject

import scala.util.parsing.input.Positional

//Definition of the abstract syntax tree for querys
sealed trait QueryAST extends Positional
case class AndThen(step1: QueryAST, step2: QueryAST) extends QueryAST

sealed trait Combinations { def step: QueryAST }
case class A(step1: QueryAST, step2: QueryAST) extends QueryAST //AND
case class O(step1: QueryAST, step2: QueryAST) extends QueryAST //OR

sealed trait Conditions extends QueryAST
case class EQ(entity: IDENTIFIER, field: IDENTIFIER) extends Conditions
case class LT(entity: IDENTIFIER, field: IDENTIFIER) extends Conditions
case class LE(entity: IDENTIFIER, field: IDENTIFIER) extends Conditions

object QueryAST{
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