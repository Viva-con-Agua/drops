package utils.Query

import scala.util.parsing.combinator.RegexParsers

case class QueryLexerError(msg: String)

object QueryLexer extends RegexParsers {
  def apply(query : String): Either[QueryLexerError, List[QueryToken]] = {
    parse(tokens, query) match {
      case NoSuccess(msg, next) => Left(QueryLexerError(msg))
      case Success(result, next) => Right(result)
    }
  }

  def tokens : Parser[List[QueryToken]] = {
    phrase(rep1(seqarator | identifier | lessEqual | equals | lessThen  | and | or ))
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

  def and       =   "_AND_"   ^^ (_ => AND)
  def or        =   "_OR_"    ^^ (_ => OR)
  def equals    =   "="       ^^ (_ => EQUALS)
  def lessThen  =   "<"       ^^ (_ => LESS_THEN)
  def lessEqual =   "<="      ^^ (_ => LESS_EQUAL)
  def seqarator =   "."       ^^ (_ => SEPARATOR)
}

