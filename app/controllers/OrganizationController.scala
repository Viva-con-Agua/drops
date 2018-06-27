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
import java.util.UUID
import play.api.libs.json.{JsPath, Json, OWrites, Reads}

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
      organizationService.find(request.body.name).flatMap { 
        case Some(orga) => Future.successful(BadRequest(Messages("organization.error.exist")))
        case _ => {
          organizationService.save(request.body.toOrganization).flatMap {
            case Some(orga) => Future.successful(Ok(Json.toJson(orga)))
            case _ => Future.successful(BadRequest("error"))
          }
        }
      }
    }

    def addProfile = Action.async(validateJson[ProfileOrganization]) { implicit request =>
      organizationService.checkProfileOranization(request.body.email, request.body.publicId).flatMap {
        case true => Future.successful(BadRequest(Messages("organization.error.profileInv")))
        case false => {
          organizationService.addProfile(request.body.email, request.body.publicId, request.body.role).flatMap {
            case Some(orga) => Future.successful(Ok(Json.toJson(orga)))
            case _ => Future.successful(BadRequest("error"))
          }
        }
      }
    }

    def addProfileName = Action.async(validateJson[ProfileOrganizationName]) { implicit request =>
      organizationService.checkProfileOranization(request.body.email, request.body.name).flatMap {
        case true => Future.successful(BadRequest(Messages("organization.error.profileInv")))
        case false => {
          organizationService.addProfile(request.body.email, request.body.name, request.body.role).flatMap {
            case Some(orga) => Future.successful(Ok(Json.toJson(orga)))
            case _ => Future.successful(BadRequest("error"))
          }
        }
      }
    }

    def getOrganization = Action.async(validateJson[OrganizationUUID]) { implicit request => 
      organizationService.find(request.body.publicId).flatMap {
        case Some(orga) => Future.successful(Ok(Json.toJson(orga)))
        case _ => Future.successful(BadRequest("error"))
      }       
    }

    def getOrganizationWithProfile = Action.async(validateJson[OrganizationUUID]) { implicit request => 
      organizationService.withProfile(request.body.publicId).flatMap {
        case Some(orga) => Future.successful(Ok(Json.toJson(orga)))
        case _ => Future.successful(BadRequest("error"))
      }
    }

    def updateOrganization = Action.async(validateJson[Organization]) { implicit request => 
      organizationService.update(request.body).flatMap {
        case Some(org) => Future.successful(Ok(Json.toJson(org)))
        case _ => Future.successful(BadRequest("error"))
      }
    }

    def delete = Action.async(validateJson[OrganizationUUID]) { implicit request =>
      organizationService.find(request.body.publicId).flatMap {
        case Some (orga) => {
          organizationService.delete(request.body.publicId)
          Future.successful(Ok)
        }
        case _ => Future.successful(BadRequest(Messages("organization.error.exist")))
      }
    }

    def deleteProfile = Action.async(validateJson[ProfileOrganization]) { implicit request =>
      organizationService.checkProfileOranization(request.body.email, request.body.publicId).flatMap {
        case false => Future.successful(BadRequest(Messages("organization.error.profileInv")))
        case true => {
          organizationService.deleteProfile(request.body.publicId, request.body.email)
          Future.successful(Ok)
        }
      }
    }

    def addBankaccount = Action.async(validateJson[BankaccountOrganization]) { implicit request =>
      organizationService.find(request.body.publicId).flatMap {
        case Some (orga) => {
          organizationService.addBankaccount(request.body.bankaccount, request.body.publicId)
          Future.successful(Ok)
        }
        case _ => Future.successful(BadRequest(Messages("error")))
      }
    }
   def addBankaccountName = Action.async(validateJson[BankaccountOrganizationName]) { implicit request =>
      organizationService.find(request.body.name).flatMap {
        case Some (orga) => {
          organizationService.addBankaccount(request.body.bankaccount, request.body.name)
          Future.successful(Ok)
        }
        case _ => Future.successful(BadRequest(Messages("error")))
      }
    }

    def withBankaccounts = Action.async(validateJson[OrganizationUUID]) { implicit request =>
      organizationService.find(request.body.publicId).flatMap {
        case Some (orga) => {
          organizationService.withBankaccounts(request.body.publicId).flatMap {
            case Some (orga) => Future.successful(Ok(Json.toJson(orga)))
            case _ => Future.successful(BadRequest("error"))
          }
        }
        case _ => Future.successful(BadRequest("error"))
      }
    }
}
