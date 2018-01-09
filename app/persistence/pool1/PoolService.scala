package persistence.pool1

import javax.inject.Inject

import models.User
import play.api.i18n.Messages

import scala.concurrent.Future
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext.Implicits.global

trait PoolService {
  def save(user: User)(implicit messages: Messages) : Future[Boolean]
}

class PoolServiceImpl @Inject() (api: PoolApi, configuration: Configuration) extends PoolService {
  val poolLogger: Logger = Logger(this.getClass())

  override def save(user: User)(implicit messages: Messages): Future[Boolean] = PoolAction(false, user) { user => {
    val container = PoolUserData(configuration.getString("pool1.hash").getOrElse(""), user)
    api.create[User](container).map(_ match {
      case Left(v) => {
        Logger.debug(Messages("pool1.debug.export.success", v.toString))
        true
      }
      case Right(e) => {
        Logger.debug(Messages("pool1.debug.export.failure", e.getMessage()))
        false
      }
    })
  }}

  private def PoolAction[T](defaultResult: T, param: User)(body: (User) => Future[T])(implicit messages: Messages) : Future[T] = {
    configuration.getBoolean("pool1.export").getOrElse(false) match {
      case true => body(param)
      case false => {
        Logger.debug(Messages("pool1.debug.not.activated"))
        Future.successful(defaultResult)
      }
    }
  }
}
