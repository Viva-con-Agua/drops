package utils.Query

import play.api.libs.json.JsObject

import scala.util.parsing.input.Positional

//Definition of the abstract syntax tree for querys
sealed trait QueryAST extends Positional {
  //ToDo: function should return a SQLActionBuilder instance (SQL Injection)
  def toSqlStatement :String
}
case class AndThen(step1: QueryAST, step2: QueryAST) extends QueryAST {
  override def toSqlStatement = step1.toSqlStatement + " AND " + step2.toSqlStatement
}

sealed trait Combinations { def step: QueryAST }
case class A(step1: QueryAST, step2: QueryAST) extends QueryAST {
  override def toSqlStatement = step1.toSqlStatement + " AND " + step2.toSqlStatement
} //AND
case class O(step1: QueryAST, step2: QueryAST) extends QueryAST {
  override def toSqlStatement = ???
} //OR

sealed trait Conditions extends QueryAST
case class EQ(entity: IDENTIFIER, field: IDENTIFIER, value: String) extends Conditions{
  def toSqlStatement = entity.str + "_" + field.str + " = \""+ value + "\""
}
case class LT(entity: IDENTIFIER, field: IDENTIFIER) extends Conditions {
  override def toSqlStatement = ???
}
case class LE(entity: IDENTIFIER, field: IDENTIFIER) extends Conditions {
  override def toSqlStatement = ???
}
case class LIKE(entity: IDENTIFIER, field: IDENTIFIER, value: String) extends Conditions {



  override def toSqlStatement = entity.str + "_" + field.str + " LIKE \"" + value + "\""
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