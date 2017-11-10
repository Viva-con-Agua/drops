package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api._
import play.api.mvc._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}

import scala.concurrent.ExecutionContext.Implicits.global
import daos.UserCiviCRMDao
import models.User

import scala.concurrent.Future

/**
  * Created by johann on 17.09.17.
  */
class Users @Inject() (
                        usersCiviDao : UserCiviCRMDao,
                       val messagesApi: MessagesApi,
                       val env:Environment[User,CookieAuthenticator]) extends  Silhouette[User,CookieAuthenticator] {
  def civiUsers = SecuredAction.async { implicit request =>
    usersCiviDao.getAll.map((list) => Ok( "There are: " + list.size + " users\n\n" + list.mkString("\n") ))
  }

  def saveMeToCivi = SecuredAction.async { implicit request =>
    usersCiviDao.save(request.identity).map((list) => Ok( "Users were saved in CiviCRM:\n\n" + list.mkString("\n")))
  }
}
