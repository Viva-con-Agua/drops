package utils

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.{Role, User}
import play.api.i18n._
import play.api.mvc.{Request, RequestHeader}

import scala.concurrent.Future

/**
  * Check for authorization
  */
case class WithRole(role: Role) extends Authorization[User,CookieAuthenticator] {
  def isAuthorized[B](user: User, authenticator: CookieAuthenticator)(implicit request : Request[B], messages: Messages) =
    user.roles match {
      case list: Set[Role] => Future.successful(list.contains(role))
      case _               => Future.successful(false)
    }

}
