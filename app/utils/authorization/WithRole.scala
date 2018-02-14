package utils.authorization

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.{Role, User}
import play.api.i18n._
import play.api.mvc.Request

import scala.concurrent.Future

/**
  * Check for authorization
  */
case class WithRole(role: Role) extends Authorization[User,CookieAuthenticator] with CombinableRestriction {
  def isAuthorized[B](user: User, authenticator: CookieAuthenticator)(implicit request : Request[B], messages: Messages) =
    user.roles match {
      case list: Set[Role] => Future.successful(list.contains(role))
      case _               => Future.successful(false)
    }
}

case class WithAlternativeRoles(role: Role*) extends Authorization[User, CookieAuthenticator] {
  override def isAuthorized[B](identity: User, authenticator: CookieAuthenticator)(implicit request: Request[B], messages: Messages): Future[Boolean] =
    Future.successful(role.foldLeft[Boolean](false)((contains, r) => identity.roles.contains(r) || contains))
}