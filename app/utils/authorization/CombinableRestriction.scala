package utils.authorization

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.User
import play.api.i18n.Messages
import play.api.mvc.Request
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

case class AuthCombination(one: CombinableRestriction, two: CombinableRestriction) extends Authorization[User,CookieAuthenticator] {
  override def isAuthorized[B](identity: User, authenticator: CookieAuthenticator)(implicit request: Request[B], messages: Messages): Future[Boolean] =
    one.isAuthorized(identity, authenticator).flatMap(
      (first) => two.isAuthorized(identity, authenticator).map(
        (second) => first && second
      )
    )
}

trait CombinableRestriction extends Authorization[User,CookieAuthenticator] {
  def isAuthorized[B](identity: User, authenticator: CookieAuthenticator)(implicit request: Request[B], messages: Messages): Future[Boolean]

  def &&(other: CombinableRestriction) : Authorization[User,CookieAuthenticator] = {
    AuthCombination(this, other)
  }
}
