package controllers

import java.util.UUID
import javax.inject.Inject

import scala.concurrent.Future
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api._
import play.api.mvc._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import models._
import play.api.data.Form
import play.api.data.Forms._
import services.UserService
import utils.authorization.{Pool1Restriction, WithRole}

import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by johann on 14.12.16.
  */
class Roles @Inject() (
  userService: UserService,
  val messagesApi: MessagesApi,
  configuration: Configuration,
  val env:Environment[User,CookieAuthenticator]) extends Silhouette[User,CookieAuthenticator] {

  val pool1Export = configuration.getBoolean("pool1.export").getOrElse(false)

  def index = SecuredAction(WithRole(RoleAdmin)).async { request =>
    userService.list.map(users => Ok(views.html.roles.index(request.identity, request.authenticator.loginInfo, RolesForms.setUsers(users))(request, messagesApi.preferred(request)))) //RolesForms.setUsers(users)
  }

  def update = SecuredAction(WithRole(RoleAdmin)).async { request =>
    RolesForms.set.bindFromRequest()(request).fold(
      bogusForm => Future.successful(BadRequest(
        views.html.roles.index(request.identity, request.authenticator.loginInfo, bogusForm)(request, messagesApi.preferred(request))
      )),
      usersRoles => {
        usersRoles.usersRoles.map((userRoles) =>
          userService.find(userRoles.userId).map(
            optUser => optUser.map((user) => userService.update(user.setRoles(userRoles.roles.get)))
          )
        )
        Future.successful(Redirect("/roles"))
      }
    )
  }
}

object RolesForms {
  case class RolesData(admin: Boolean, employee: Boolean, supporter: Boolean) {
    def get : Set[Role] = Map(
      "admin" -> admin,
      "employee" -> employee,
      "supporter" -> supporter
    ).foldLeft[Set[Role]](Set())(
      (res, pair) => pair._2 match {
        case true => res + Role.apply(pair._1)
        case _ => res
      }
    )
  }
  case class UserRoles(userId:UUID, roles: RolesData)
  case class UsersRoles(usersRoles: List[UserRoles])

  def set = Form(mapping(
    "users" -> list(mapping(
      "userId" -> nonEmptyText,
      "roles" -> mapping(
        "admin" -> boolean,
        "employee" -> boolean,
        "supporter" -> boolean
      )(RolesData.apply)(RolesData.unapply)
    )
    ((userId, roles) => UserRoles(UUID.fromString(userId), roles))
    ((raw) => Some((raw.userId.toString, raw.roles)))
  ))(UsersRoles.apply)(UsersRoles.unapply))

  def setUsers(users: List[User]) = this.set.fill(UsersRoles(users.map((user) =>
    UserRoles(user.id, RolesData(
      user.roles.contains(RoleAdmin),
      user.roles.contains(RoleEmployee),
      user.roles.contains(RoleSupporter)
    ))
  )))
}