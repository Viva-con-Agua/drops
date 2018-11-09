package utils.Query

import scala.util.parsing.input.Positional

//Definition of all possible query elements
//ToDo: Add all signs
sealed trait QueryToken extends Positional

//ToDo: Add dot separator or use different definitions für entity and key
case class IDENTIFIER(str: String) extends QueryToken
case class VALUE(str: String) extends QueryToken
case class INDEX(i: Int) extends QueryToken


case object SEPARATOR extends QueryToken

sealed trait LogicalOperator extends QueryToken
case object AND extends LogicalOperator
case object OR extends LogicalOperator


sealed trait QueryOperator extends QueryToken
case object EQUALS        extends QueryOperator
case object LESS_THEN     extends QueryOperator
case object LESS_EQUAL    extends QueryOperator
case object GREATER_THEN  extends QueryOperator
case object GREATER_EQUAL extends QueryOperator
case object LIKE_OPERATOR extends QueryOperator
case object BETWEEN_OPERATOR extends QueryOperator

sealed trait GroupOperator extends QueryToken
case object OPEN_BRACKET extends GroupOperator
case object CLOSE_BRACKET extends GroupOperator