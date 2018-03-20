package controllers

import javax.inject.Inject

import play.api._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global

import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api.{Environment, LoginInfo, Silhouette}
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import models._

class OrganizationController @Inject() (
      val messagesApi: MessagesApi,
      val env:Environment[User,CookieAuthenticator]
  )extends Silhouette[User,CookieAuthenticator] {
    
    def index = SecuredAction.async { implicit request => ??? }

    def insert = SecuredAction.async { implicit request => ??? }
  
  }
