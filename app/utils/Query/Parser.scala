package utils.Query

import java.security.SecurityPermission

import models.views.ViewBase
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

case class QueryParser(
                      values: JSONObject
                      )

object QueryParser extends Parsers{
  override type Elem = QueryToken

  var values: ViewBase = null

  class QueryTokenReader(tokens: Seq[QueryToken],values: ViewBase) extends Reader[QueryToken]{
    override def first: QueryToken = tokens.head
    override def rest: Reader[QueryToken] = new QueryTokenReader(tokens.tail, values: ViewBase)
    override def pos: Position = tokens.headOption.map(t => t.pos).getOrElse(NoPosition)
    override def atEnd: Boolean = tokens.isEmpty
  }

  def apply(tokens: Seq[QueryToken], values: ViewBase): Either[QueryParserError, QueryAST] = {
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
    rep1(statement) ^^ { case stmtList => stmtList reduceRight AndThen }
  }

  def statement: Parser[QueryAST] = {
     and | or | eq  | lt | le | gt | ge | like
  }


  private def entity: Parser[IDENTIFIER] = {
    accept("entity identifier", { case e @ IDENTIFIER(str) => e })
  }
  private def field: Parser[IDENTIFIER] = {
    accept("field identifier", { case f @ IDENTIFIER(str) => f })
  }

  def and : Parser[A] = {
    ((eq | lt | le | gt | ge | like) ~ AND ~ (eq | lt | le | gt | ge | like)) ^^ {
      case f1 ~ _ ~ f2  => A(f1, f2)
    }
  }

  def or : Parser[O] = {
    ((eq | lt | le | gt | ge | like) ~ OR ~ (eq | lt | le | gt | ge | like)) ^^ {
      case f1 ~ _ ~ f2 => O(f1, f2)
    }
  }

  def eq: Parser[Conditions] = positioned {
    (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ EQUALS)     ^^ { case entity ~ _ ~  field ~ _ ~ _ => EQ(entity, field, getValue(entity.str, field.str)) }
  }

  def lt: Parser[Conditions] = positioned {
    (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ LESS_THEN)  ^^ { case entity ~ _ ~  field ~ _ ~ _ => LT(entity, field, getValue(entity.str, field.str)) }
  }

  def le: Parser[Conditions] = positioned {
    (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ LESS_EQUAL) ^^ { case entity ~ _ ~  field ~ _ ~ _ => LE(entity, field, getValue(entity.str, field.str)) }
  }

  def gt: Parser[Conditions] = positioned {
    (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ LESS_THEN)  ^^ { case entity ~ _ ~  field ~ _ ~ _ => GT(entity, field, getValue(entity.str, field.str)) }
  }

  def ge: Parser[Conditions] = positioned {
    (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ LESS_EQUAL) ^^ { case entity ~ _ ~  field ~ _ ~ _ => GE(entity, field, getValue(entity.str, field.str)) }
  }

  def like: Parser[Conditions] = positioned{
    (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ LIKE_OPERATOR) ^^ {case entity ~ _ ~ field ~ _ ~ _ => LIKE(entity, field, getValue(entity.str, field.str)) }
  }
  //ToDo: Add Error Handling!
  //ToDo: Use Messages
  def getValue(entity: String, field: String) : String = {
    if(values.isFieldDefined(entity)){
      val e : ViewBase = values.getValue(entity).asInstanceOf[ViewBase]
      if(e.isFieldDefined(field)){
        e.getValue(field).toString
      }else{
        throw new QueryParserError("There is no filter value for one or more query parts")
      }
    }else{
      throw new QueryParserError("There is no filter value for one or more query parts")
    }
  }
}
