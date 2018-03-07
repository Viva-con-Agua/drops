package utils.authorization

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.{Role, RoleAdmin, User}
import play.api.i18n.Messages
import play.api.mvc.Request

import scala.concurrent.Future

/**
  * Gets a parameter indicating if an active connection to Pool 1 exists. If the parameter is true (connection exists):
  * Only admins have permissions to access the requested resource; all other users are rejected. If it's false, there is
  * no need to consider this restriction.
  *
  * @author Johann Sell
  * @param active indicates if a connection does exists
  */
case class Pool1Restriction(active: Boolean) extends Authorization[User,CookieAuthenticator] with CombinableRestriction {
  def isAuthorized[B](user: User, authenticator: CookieAuthenticator)(implicit request : Request[B], messages: Messages) =
    user.roles match {
      case list: Set[Role] => Future.successful(list.contains(RoleAdmin) || !active)
      case _               => Future.successful(!active)
    }
}
