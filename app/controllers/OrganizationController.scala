package controllers

import javax.inject.Inject

import play.api._
import play.api.mvc._

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

    def insert = SecuredAction.async { implicit request =>
      Future.successful(Ok(dispenserService.getTemplate(views.html.organization.insertOrganization(OrganizationForms.organizationForm))))
    
    }

    def handleInsertOrganization = SecuredAction.async { implicit request =>
      OrganizationForms.organizationForm.bindFromRequest.fold(
        bogusForm => Future.successful(
          BadRequest(dispenserService.getTemplate(views.html.organization.insertOrganization(bogusForm)))
        ),
      organizationData => {
        //what identify an organization?
        //case None => 
          val organizationStub = OrganizationStub(
            organizationData.name, 
            organizationData.address, 
            organizationData.telefon, 
            organizationData.fax,
            organizationData.email,
            organizationData.executive,
            organizationData.abbreviation,
            organizationData.impressum,
            None
          )
        for {
          organization <- organizationService.save(organizationStub.toOrganization)
        }yield{
          Ok(dispenserService.getTemplate(views.html.organization.insertOrganization(OrganizationForms.organizationForm)))
        }
      } 
     
    )
  }
}
