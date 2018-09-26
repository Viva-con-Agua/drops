package controllers.rest

import daos.{MariadbUserDao, UserDao}
import models.dbviews.Users
import models.User
import play.api.i18n.Messages
import play.api.libs.json.Json
import utils.Query._

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

case class QueryBody(
                 query: Option[String],
                 values: Option[Users],
                 sort : Option[String],
                 offset: Option[Long],
                 limit: Option[Long]
                    )
object QueryBody {
  implicit val queryBodyFormat = Json.format[QueryBody]

  case class NoValuesGiven(msg: String) extends Exception(msg)
  case class NoQueryGiven(msg: String) extends Exception(msg)

  def asUserRequest(body: QueryBody)(implicit userDao: MariadbUserDao, messages: Messages) : Future[Either[Exception, List[User]]] = {
    val page = body.limit.map((l) => Page(l, body.offset))

    body.query.map(QueryLexer( _ ) match {
      case Left(error) => Left(error)
      case Right(tokens) => body.values.map(QueryParser(tokens, _ ) match {
        case Left(error) => Left(error)
        case Right(ast) => Right(ast)
      }).getOrElse(Left(NoValuesGiven(Messages("rest.api.noValuesGiven"))))
    }).getOrElse(Left(NoQueryGiven(Messages("rest.api.noQueryGiven")))) match { // got either an error or an AST
      case Left(error: NoQueryGiven) => {
        val stmt = Converter("Users", None, page)
        try {
          userDao.list_with_statement(stmt.toStatement).map(Right( _ )) //.map(users => Ok(Json.toJson(users)))
        } catch {
          case e: java.sql.SQLException => {
            Future.successful(Left(e))
          }
          case e: Exception => {
            Future.successful(Left(e))
          }
        }
      }
      case Left(error : Exception) => Future.successful(Left(error))
      case Right(ast) => {
        val stmt = Converter("Users", Some(ast), page)
        try {
          userDao.list_with_statement(stmt.toStatement).map(Right( _ ))
        } catch {
          case e: java.sql.SQLException => {
            Future.successful(Left(e))
          }
          case e: Exception => {
            Future.successful(Left(e))
          }
        }
      }
    }
  }

  def getUsersCount(body: QueryBody)(implicit userDao: MariadbUserDao, messages: Messages) : Future[Either[Exception, Long]] = {
    body.query.map(QueryLexer( _ ) match {
      case Left(error) => Left(error)
      case Right(tokens) => body.values.map(QueryParser(tokens, _ ) match {
        case Left(error) => Left(error)
        case Right(ast) => Right(ast)
      }).getOrElse(Left(NoValuesGiven(Messages("rest.api.noValuesGiven"))))
    }).getOrElse(Left(NoQueryGiven(Messages("rest.api.noQueryGiven")))) match { // got either an error or an AST
      case Left(error: NoQueryGiven) => {
        val stmt = Converter("Users", None, None)
        try {
          userDao.count_with_statement(stmt.toCountStatement).map(Right( _ )) //.map(users => Ok(Json.toJson(users)))
        } catch {
          case e: java.sql.SQLException => {
            Future.successful(Left(e))
          }
          case e: Exception => {
            Future.successful(Left(e))
          }
        }
      }
      case Left(error : Exception) => Future.successful(Left(error))
      case Right(ast) => {
        val stmt = Converter("Users", Some(ast), None)
        try {
          userDao.count_with_statement(stmt.toStatement).map(Right( _ ))
        } catch {
          case e: java.sql.SQLException => {
            Future.successful(Left(e))
          }
          case e: Exception => {
            Future.successful(Left(e))
          }
        }
      }
    }
  }
}