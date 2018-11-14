package utils.authorization

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models._
import play.api.i18n._
import play.api.mvc.Request

import scala.concurrent.Future

/**
  * Check for authorization
  */
case class WithRole(role: Role) extends Authorization[User,CookieAuthenticator] with CombinableRestriction {
  def isAuthorized[B](user: User, authenticator: CookieAuthenticator)(implicit request : Request[B], messages: Messages) =
    Future.successful(
      user.roles.contains(role) ||
        user.profiles.foldLeft(false)((hasRole, profile) =>
          hasRole || profile.supporter.roles.contains(role)
        )
    )
}

trait VolunteerManagerFinder {
  def check(user: User, checker: VolunteerManager => Boolean): Boolean = user.profiles.foldLeft(false)((isVM, profile) =>
    isVM || profile.supporter.roles.foldLeft(false)((containsVM, role) => role match {
      case vm: VolunteerManager => containsVM || checker(vm)
      case _ => containsVM || false
    })
  )
}

case class IsVolunteerManager(crewName: Option[String] = None) extends Authorization[User, CookieAuthenticator] with CombinableRestriction with VolunteerManagerFinder {
  override def isAuthorized[B](identity: User, authenticator: CookieAuthenticator)(implicit request: Request[B], messages: Messages): Future[Boolean] =
    Future.successful(check(identity, (vm: VolunteerManager) => crewName.map(vm.forCrew( _ )).getOrElse(true)))
}

object IsVolunteerManager {
  def apply(crew: Option[Crew]): IsVolunteerManager = IsVolunteerManager(crew.map(_.name))
  def apply(crew: Crew): IsVolunteerManager = IsVolunteerManager(Some(crew.name))
  def apply(crewName: String): IsVolunteerManager = IsVolunteerManager(Some(crewName))
}

case class IsResponsibleFor(pillar: Pillar) extends Authorization[User, CookieAuthenticator] with CombinableRestriction with VolunteerManagerFinder {
  override def isAuthorized[B](identity: User, authenticator: CookieAuthenticator)(implicit request: Request[B], messages: Messages): Future[Boolean] =
    Future.successful(check(identity, (vm: VolunteerManager) => vm.isResponsibleFor(pillar)))
}

case class IsVolunteerManagerFor(crewName: String, pillar: Pillar) extends Authorization[User, CookieAuthenticator] with CombinableRestriction with VolunteerManagerFinder {
  override def isAuthorized[B](identity: User, authenticator: CookieAuthenticator)(implicit request: Request[B], messages: Messages): Future[Boolean] =
    Future.successful(check(identity, (vm: VolunteerManager) => vm.forCrew( crewName ) && vm.isResponsibleFor( pillar )))
}

object IsVolunteerManagerFor {
  def apply(crew: Crew, pillar: Pillar): IsVolunteerManagerFor = IsVolunteerManagerFor(crew.name, pillar)
}

/**
  * @deprecated - use WithRole() || WithRole() instead
  * @param role
  */
case class WithAlternativeRoles(role: Role*) extends Authorization[User, CookieAuthenticator] {
  override def isAuthorized[B](identity: User, authenticator: CookieAuthenticator)(implicit request: Request[B], messages: Messages): Future[Boolean] =
    role.foldLeft[Future[Boolean]](Future.successful(false))((hasRole, r) =>
      hasRole.flatMap(hR => WithRole(r).isAuthorized(identity, authenticator).map(_ || hR))
    )
}