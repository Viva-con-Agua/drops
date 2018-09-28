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
                 sort : Option[Sort],
                 offset: Option[Long],
                 limit: Option[Long]
                    )
object QueryBody {
  implicit val queryBodyFormat = Json.format[QueryBody]

  case class NoValuesGiven(msg: String) extends Exception(msg)
  case class NoQueryGiven(msg: String) extends Exception(msg)

  private def asRequest(body: QueryBody, view: String, ignorePagination : Boolean = false)(implicit messages: Messages) : Either[Exception, Converter] = {
    val page = if(!ignorePagination) body.limit.map((l) => Page(l, body.offset)) else None

    body.query.map(QueryLexer( _ ) match {
      case Left(error) => Left(error)
      case Right(tokens) => body.values.map(QueryParser(tokens, _ ) match {
        case Left(error) => Left(error)
        case Right(ast) => Right(ast)
      }).getOrElse(Left(NoValuesGiven(Messages("rest.api.noValuesGiven"))))
    }).getOrElse(Left(NoQueryGiven(Messages("rest.api.noQueryGiven")))) match { // got either an error or an AST
      case Left(error: NoQueryGiven) => {
        Right(Converter(view, None, page, body.sort))
      }
      case Left(error : Exception) => Left(error)
      case Right(ast) => {
        Right(Converter(view, Some(ast), page, body.sort))
      }
    }
  }

  def asUsersQuery(body: QueryBody)(implicit messages: Messages) : Either[Exception, Converter] = asRequest(body, "Users")
  def asUsersCountQuery(body: QueryBody)(implicit messages: Messages) : Either[Exception, Converter] = asRequest(body, "Users", true)

  def asCrewsQuery(body: QueryBody)(implicit messages: Messages) : Either[Exception, Converter] = asRequest(body, "Crews")
  def asCrewsCountQuery(body: QueryBody)(implicit messages: Messages) : Either[Exception, Converter] = asRequest(body, "Crews", true)

  def asTasksQuery(body: QueryBody)(implicit messages: Messages) : Either[Exception, Converter] = asRequest(body, "Tasks")
  def asTasksCountQuery(body: QueryBody)(implicit messages: Messages) : Either[Exception, Converter] = asRequest(body, "Tasks", true)
}