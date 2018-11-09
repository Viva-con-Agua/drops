package utils.Query

import java.security.SecurityPermission

import models.dbviews.{ViewBase, ViewObject}
import net.minidev.json.JSONObject
import play.api.i18n.Messages
import play.api.libs.json.JsObject


import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.{NoPosition, Position, Reader}

//case class QueryParserError(msg: String)
class QueryParserError(message: String) extends Exception(message) {
  def this(message: String, cause: Throwable) {
    this(message)
    initCause(cause)
  }

  def this(cause: Throwable) {
    this(Option(cause).map(_.toString).orNull, cause)
  }

  def this() {
    this(null: String)
  }
}

//ToDo: Values so hier noch notwendig?
case class QueryParser(
                      values: JSONObject
                      )

object QueryParser extends Parsers{
  override type Elem = QueryToken

  var values: ViewObject = null

  class QueryTokenReader(tokens: Seq[QueryToken],values: ViewObject) extends Reader[QueryToken]{
    override def first: QueryToken = tokens.head
    override def rest: Reader[QueryToken] = new QueryTokenReader(tokens.tail, values: ViewObject)
    override def pos: Position = tokens.headOption.map(t => t.pos).getOrElse(NoPosition)
    override def atEnd: Boolean = tokens.isEmpty
  }

  def apply(tokens: Seq[QueryToken], values: ViewObject): Either[QueryParserError, QueryAST] = {
    this.values = values
    val reader = new QueryTokenReader(tokens, values)
    program(reader) match {
      case NoSuccess(msg, next) => Left(new QueryParserError(msg))
      case Success(result, next) => Right(result)
    }
  }

  def program: Parser[QueryAST] = {
    phrase(block)
  }

  def block: Parser[QueryAST] = {
    rep1(statement) ^^ { case stmtList => stmtList reduceRight A }
  }

  def statement: Parser[QueryAST] = {
    combined_group | group | expression | eq  | lt | le | gt | ge | like | between
  }


  private def entity: Parser[IDENTIFIER] = {
    accept("entity identifier", { case e @ IDENTIFIER(str) => e })
  }
  private def field: Parser[IDENTIFIER] = {
    accept("field identifier", { case f @ IDENTIFIER(str) => f })
  }

  private def index: Parser[INDEX] = {
    accept("index identifier", { case f @ INDEX(i) => f })
  }


  def expression : Parser[Combination] = {
    ((eq  | lt | le | gt | ge | like | between) ~ rep((OR | AND) ~ (eq  | lt | le | gt | ge | like | between))) ^^ {
      case f1 ~ f2 => Combination(f1, f2)
    }
  }

  def group : Parser[Group] = {
    (OPEN_BRACKET ~ expression ~ CLOSE_BRACKET) ^^ { case  f1 ~ f2 ~ f3 => Group(f1, f2, f3)}
  }



  def combined_group : Parser[CombinedGroup] = {
    ((expression | group) ~ rep(( OR | AND ) ~ (expression | group))) ^^ {
      case f1 ~ f2  => CombinedGroup(f1, f2)
    }
  }

  def eq: Parser[Conditions] = positioned {
    (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ EQUALS) ^^ { case entity ~ _ ~  field ~ _ ~ _ => EQ(entity, field, getValue(entity.str, field.str, 0)) } |
    (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ index ~ SEPARATOR ~ EQUALS)     ^^ { case entity ~ _ ~  field ~ _ ~ index ~ _ ~ _ => EQ(entity, field, getValue(entity.str, field.str, index.i)) }
  }

  def lt: Parser[Conditions] = positioned {
    (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ LESS_THEN)  ^^ { case entity ~ _ ~  field ~ _ ~ _ => LT(entity, field, getValue(entity.str, field.str, 0)) } |
    (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ index ~ SEPARATOR ~ LESS_THEN)     ^^ { case entity ~ _ ~  field ~ _ ~ index ~ _ ~ _ => LT(entity, field, getValue(entity.str, field.str, index.i)) }
  }

  def le: Parser[Conditions] = positioned {
    (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ LESS_EQUAL) ^^ { case entity ~ _ ~  field ~ _ ~ _ => LE(entity, field, getValue(entity.str, field.str, 0)) } |
    (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ index ~ SEPARATOR ~ LESS_EQUAL)     ^^ { case entity ~ _ ~  field ~ _ ~ index ~ _ ~ _ => LE(entity, field, getValue(entity.str, field.str, index.i)) }
  }

  def gt: Parser[Conditions] = positioned {
    (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ GREATER_THEN)  ^^ { case entity ~ _ ~  field ~ _ ~ _ => GT(entity, field, getValue(entity.str, field.str, 0)) } |
    (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ index ~ SEPARATOR ~ GREATER_THEN)     ^^ { case entity ~ _ ~  field ~ _ ~ index ~ _ ~ _ => GT(entity, field, getValue(entity.str, field.str, index.i)) }
  }

  def ge: Parser[Conditions] = positioned {
    (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ GREATER_EQUAL) ^^ { case entity ~ _ ~  field ~ _ ~ _ => GE(entity, field, getValue(entity.str, field.str, 0)) } |
      (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ index ~ SEPARATOR ~ GREATER_EQUAL)     ^^ { case entity ~ _ ~  field ~ _ ~ index ~ _ ~ _ => GE(entity, field, getValue(entity.str, field.str, index.i)) }
  }

  def like: Parser[Conditions] = positioned{
    (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ LIKE_OPERATOR) ^^ {case entity ~ _ ~ field ~ _ ~ _ => LIKE(entity, field, getValue(entity.str, field.str, 0)) } |
    (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ index ~ SEPARATOR ~ LIKE_OPERATOR)     ^^ { case entity ~ _ ~  field ~ _ ~ index ~ _ ~ _ => LIKE(entity, field, getValue(entity.str, field.str, index.i)) }
  }

  def between: Parser[Conditions] = positioned {
    (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ BETWEEN_OPERATOR) ^^ { case entity ~ _ ~ field ~ _ ~ _ => BETWEEN(entity, field, getValuePair(entity.str, field.str, 0)) }
    (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ index ~ SEPARATOR ~ BETWEEN_OPERATOR) ^^ { case entity ~ _ ~ field ~ _ ~ index ~ _ ~ _ => BETWEEN(entity, field, getValuePair(entity.str, field.str, index.i)) }
  }
  //ToDo: Add Error Handling!
  //ToDo: Use Messages
  def getValue(entity: String, field: String, index: Int) : String = {
    QueryParser.getValueBase(entity, field, index) match {
      case l : List[Any] => l.headOption.map(_.toString) match {
        case Some(head) => head
        case None => throw new QueryParserError("The operator expects more elements.")
      }
      case x : Any => x.toString
    }
  }

  def getValuePair(entity: String, field: String, index: Int): (String, String) = {
    QueryParser.getValueBase(entity, field, index) match {
      case l : List[Any] => {
        val str = l.map(_.toString)
        str.headOption.flatMap(h => str.tail.headOption.map( ( h, _ ) )) match {
          case Some(pair) => pair
          case _ => throw new QueryParserError("The operator expects more elements.")
        }
      }
      case _ => throw new QueryParserError("There is no list of elements given, as expected for this field.")
    }
  }

  private def getValueBase(entity: String, field: String, index: Int): Any = {
    if(values.isFieldDefined(entity)){
      val e : ViewBase = values.getValue(entity).asInstanceOf[ViewBase]
      if(e.isFieldDefined(field, index)){
        e.getValue(field, index)
      }else{
        throw new QueryParserError("There is no filter value for one or more query parts")
      }
    }else{
      throw new QueryParserError("There is no filter value for one or more query parts")
    }
  }
}
