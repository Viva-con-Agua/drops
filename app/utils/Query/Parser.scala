package utils.Query

import java.security.SecurityPermission

import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.{NoPosition, Position, Reader}

case class QueryParserError(msg: String)

object QueryParser extends Parsers{
  override type Elem = QueryToken

  class QueryTokenReader(tokens: Seq[QueryToken]) extends Reader[QueryToken]{
    override def first: QueryToken = tokens.head
    override def rest: Reader[QueryToken] = new QueryTokenReader(tokens.tail)
    override def pos: Position = tokens.headOption.map(t => t.pos).getOrElse(NoPosition)
    override def atEnd: Boolean = tokens.isEmpty
  }

  def apply(tokens: Seq[QueryToken]): Either[QueryParserError, QueryAST] = {
    val reader = new QueryTokenReader(tokens)
    program(reader) match {
      case NoSuccess(msg, next) => Left(QueryParserError(msg))
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
     and | or | eq  | lt | le
  }


  private def entity: Parser[IDENTIFIER] = {
    accept("entity identifier", { case e @ IDENTIFIER(str) => e })
  }
  private def field: Parser[IDENTIFIER] = {
    accept("field identifier", { case f @ IDENTIFIER(str) => f })
  }

  def and : Parser[A] = {
    ((eq | lt | le) ~ AND ~ (eq | lt | le)) ^^ {
      case f1 ~ _ ~ f2  => A(f1, f2)
    }
  }

  def or : Parser[O] = {
    ((eq | lt | le) ~ OR ~ (eq | lt | le)) ^^ {
      case f1 ~ _ ~ f2 => O(f1, f2)
    }
  }

  def eq: Parser[Conditions] = positioned {
    (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ EQUALS)     ^^ { case entity ~ _ ~  field ~ _ ~ _ => EQ(entity, field) }
  }

  def lt: Parser[Conditions] = positioned {
    (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ LESS_THEN)  ^^ { case entity ~ _ ~  field ~ _ ~ _ => LT(entity, field) }
  }

  def le: Parser[Conditions] = positioned {
    (entity ~ SEPARATOR ~ field ~ SEPARATOR ~ LESS_EQUAL) ^^ { case entity ~ _ ~  field ~ _ ~ _ => LE(entity, field) }
  }
}
