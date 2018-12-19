package persistence.pool1

import javax.inject.Inject
import java.util.UUID

import models.User
import play.api.i18n.Messages

import scala.concurrent.Future
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext.Implicits.global

trait PoolService {
  def activated: Boolean
  def save(user: User)(implicit messages: Messages) : Future[Boolean]
  def read(id: UUID)(implicit messages: Messages) : Future[Option[PoolMailSwitch]]
  def read(user: User)(implicit messages: Messages) : Future[Option[PoolMailSwitch]]
  def update(user: User, mailSwitch: Option[String] = None)(implicit messages: Messages) : Future[Boolean]
  def delete(id: UUID)(implicit messages: Messages) : Future[Boolean]
  def delete(user: User)(implicit messages: Messages) : Future[Boolean]
  def logout(user: User)(implicit messages: Messages) : Future[Boolean]
}

class PoolServiceImpl @Inject() (api: PoolApi, configuration: Configuration) extends PoolService {
  val poolLogger: Logger = Logger(this.getClass())

  private def readResponse[T]
    (response: PoolResponseData, callback: (PoolResponseData, Boolean) => T)(implicit messages: Messages) : T =
    response.code match {
    case 200 => {
      Logger.debug(Messages("pool1.debug.export.success", response.toString))
      callback(response, true)
    }
    case _ => {
      Logger.debug(Messages("pool1.debug.export.failure",
        Messages("pool1.debug.export.errorResponseCode", response.code, response.message)))
      callback(response, false)
    }
  }

  override def save(user: User)(implicit messages: Messages): Future[Boolean] = PoolAction(false, user) { user => {
    PoolRequest.create(configuration.getString("pool1.hash").getOrElse(""), user).map(request =>
      api.create(request).map(_ match {
        case Left(response) => readResponse[Boolean](response, (_, success) => success)
        case Right(e) => {
          Logger.debug(Messages("pool1.debug.export.failure", e.getMessage()))
          false
        }
      })).getOrElse({
        Logger.debug(Messages("pool1.debug.export.failure", Messages("pool1.debug.export.userNotComplete")))
        Future.successful(false)
      })
  }}

  override def read(id: UUID)(implicit messages: Messages): Future[Option[PoolMailSwitch]] = PoolAction[Option[PoolMailSwitch], UUID](None, id) { id => {
    PoolRequest.read(configuration.getString("pool1.hash").getOrElse(""), id).map(request =>
      api.read(request).map(_ match {
        case Left(response) => readResponse[Option[PoolMailSwitch]](response, (r, success) => success match {
          case true => r.response
          case false => None
        })
        case Right(e) => {
          Logger.debug(Messages("pool1.debug.export.failure", e.getMessage()))
          None
        }
      })).getOrElse({
      Logger.debug(Messages("pool1.debug.export.failure", Messages("pool1.debug.export.userNotComplete")))
      Future.successful(None)
    })
  }}

  override def read(user: User)(implicit messages: Messages): Future[Option[PoolMailSwitch]] =
    PoolAction[Option[PoolMailSwitch], User](None, user) { user =>
      read(user.id)
    }

  override def update(user: User, mailSwitch: Option[String] = None)(implicit messages: Messages): Future[Boolean] = PoolAction(false, user) { user => {
    PoolRequest.update(configuration.getString("pool1.hash").getOrElse(""), user, mailSwitch).map(request =>
      api.update(request).map(_ match {
        case Left(response) => readResponse[Boolean](response, (_, success) => success)
        case Right(e) => {
          Logger.debug(Messages("pool1.debug.export.failure", e.getMessage()))
          false
        }
      })).getOrElse({
      Logger.debug(Messages("pool1.debug.export.failure", Messages("pool1.debug.export.userNotComplete")))
      Future.successful(false)
    })
  }}

  override def delete(id: UUID)(implicit messages: Messages): Future[Boolean] = PoolAction(false, id) { id => {
    PoolRequest.delete(configuration.getString("pool1.hash").getOrElse(""), id).map(request =>
      api.delete(request).map(_ match {
        case Left(response) => readResponse[Boolean](response, (_, success) => success)
        case Right(e) => {
          Logger.debug(Messages("pool1.debug.export.failure", e.getMessage()))
          false
        }
      })).getOrElse({
      Logger.debug(Messages("pool1.debug.export.failure", Messages("pool1.debug.export.userNotComplete")))
      Future.successful(false)
    })
  }}

  override def delete(user: User)(implicit messages: Messages): Future[Boolean] = PoolAction(false, user) { user =>
    delete(user.id)
  }

  override def logout(user: User)(implicit messages: Messages): Future[Boolean] = PoolAction(false, user) { user => {
    PoolRequest.logout(configuration.getString("pool1.hash").getOrElse(""), user).map(request =>
      api.logout(request).map(_ match {
        case Left(response) => readResponse[Boolean](response, (_, success) => success)
        case Right(e) => {
          Logger.debug(Messages("pool1.debug.export.failure", e.getMessage()))
          false
        }
    })).getOrElse({
      Logger.debug(Messages("pool1.debug.export.failure", Messages("pool1.debug.export.userNotComplete")))
      Future.successful(false)
    })
  }}

  override def activated: Boolean = configuration.getBoolean("pool1.export").getOrElse(false)

  private def PoolAction[T, P](defaultResult: T, param: P)(body: P => Future[T])(implicit messages: Messages) : Future[T] = {
    configuration.getBoolean("pool1.export").getOrElse(false) match {
      case true => body(param)
      case false => {
        Logger.debug(Messages("pool1.debug.not.activated"))
        Future.successful(defaultResult)
      }
    }
  }
}
