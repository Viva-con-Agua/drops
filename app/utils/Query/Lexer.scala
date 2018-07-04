package utils.Query

import scala.util.parsing.combinator.RegexParsers

class QueryLexerError(message: String) extends Exception(message) {
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

object QueryLexer extends RegexParsers {
  def apply(query : String): Either[QueryLexerError, List[QueryToken]] = {
    parse(tokens, query) match {
      case NoSuccess(msg, next) => Left(new QueryLexerError(msg))
      case Success(result, next) => Right(result)
    }
  }

  def tokens : Parser[List[QueryToken]] = {
    phrase(rep1(like | lessEqual | equals | lessThen | greaterThen | greaterEqual | and | or | seqarator | identifier | index | closeBracket | openBracket))
  }

  /**
    * Define the pattern for field identifier and split this
    * into model and key
    * model => database table
    * key => database field
    * @return IDENTIFIER(model, key)
    */
  def identifier: Parser[IDENTIFIER] = {
    //model.key.
    "([a-zA-Z]+)".r ^^ {str => IDENTIFIER(str)}
  }

  def index: Parser[INDEX] = {
    "([0-9]+)".r ^^ {i => INDEX(i.toInt)}
  }

  def and           =   "_AND_"   ^^ (_ => AND)
  def or            =   "_OR_"    ^^ (_ => OR)
  def equals        =   "="       ^^ (_ => EQUALS)
  def lessThen      =   "<"       ^^ (_ => LESS_THEN)
  def lessEqual     =   "<="      ^^ (_ => LESS_EQUAL)
  def greaterThen   =   ">"       ^^ (_ => GREATER_THEN)
  def greaterEqual  =   ">="      ^^ (_ => GREATER_EQUAL)
  def like          =   "LIKE"    ^^ (_ => LIKE_OPERATOR)
  def seqarator     =   "."       ^^ (_ => SEPARATOR)
  def openBracket   =   "("       ^^ (_ => OPEN_BRACKET)
  def closeBracket  =   ")"       ^^ (_ => CLOSE_BRACKET)
}

