package controllers

import javax.inject.Inject

import play.api._
import play.api.mvc._
import play.api.Play.current
import play.api.libs.iteratee
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api.{Environment, LoginInfo, Silhouette}
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import models._
import models.forms.OrganizationForms
import models.dispenser._
import services._

class OrganizationController @Inject() (
      //forms: OrganizationForms,
      organizationService: OrganizationService,
      dispenserService: DispenserService,
      val messagesApi: MessagesApi,
      val env:Environment[User,CookieAuthenticator]
  )extends Silhouette[User,CookieAuthenticator] {
    
    
    def index = SecuredAction.async { implicit request => ??? }

    /*def insert = SecuredAction.async { implicit request =>
      Future.successful(Ok(dispenserService.getTemplate(views.html.organization.insertOrganization(OrganizationForms.organizationForm))))
    
    }*/
    
    def validateJson[A: Reads] = BodyParsers.parse.json.validate(_.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e)))) 

    def insert = Action.async(validateJson[OrganizationStub]) { implicit request => 
      organizationService.save(request.body.toOrganization)
      Future.successful(Ok)
    }

    def addProfile = Action.async(email: String, id: UUID) { implicit request => 
      organizationService.addProfile(email, id).flatMap {
        case Some( _ ) =>
          Future.successful(Ok( _ ))
        case None =>
          Future.successful(BadRequest("error"))
      }
    }

    def getOrganization = Action.async(id: UUID) { implicit request => 
      organizationService.find(id).flatMap
    }

    def getOrganizationWithProfile = Action.async { implicit request => ??? }

    def updateOrganization = Action.async { implicit request => ??? }

}
