package controllers

import javax.inject.Inject
import services.{UserService, DispenserService, OrganizationService}
import play.api.mvc._
import play.api._
import play.api.Play.current
import play.api.libs.iteratee
import play.api.libs.json._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api.{Environment, LoginInfo, Silhouette}
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future, Await}
import scala.concurrent.duration._
import scala.language.postfixOps
import play.twirl.api.Html
import models._
class ImprintController @Inject()(
  val messagesApi: MessagesApi,
  val env:Environment[User, CookieAuthenticator],
  dispenserService: DispenserService,
  organizationService: OrganizationService,
  userService: UserService
)extends Silhouette[User, CookieAuthenticator]{
 

  def imprint = Action.async { implicit request =>
    
    val vcaSPHtml:Future[Html] = organizationService.withBankAccounts("Viva con Agua de Sankt Pauli e.V.").flatMap {
      case Some(orga) => userService.profileListByRole(orga.publicId, "executive").flatMap {
        case Some(executive) => userService.profileListByRole(orga.publicId, "medien").flatMap {
          case Some(visdp) => userService.profileListByRole(orga.publicId, "representative").flatMap {
            case Some(profiles) => orga.bankaccount match {
              case Some(b) => Future.successful(views.html.imprints.imprintEV(orga, executive.head, profiles, visdp.head, b.head))
              case _ => Future.successful(views.html.imprints.error("Vica con Agua de Sankt Pauli e.V."))
            }
            case _ => Future.successful(views.html.imprints.error("Vica con Agua de Sankt Pauli e.V."))
          }
          case _ => Future.successful(views.html.imprints.error("Vica con Agua de Sankt Pauli e.V."))
        }
        case _ => Future.successful(views.html.imprints.error("Vica con Agua de Sankt Pauli e.V."))
      }
      case _ => Future.successful(views.html.imprints.error("Vica con Agua de Sankt Pauli e.V."))
    }

    val vcaWaterHtml:Future[Html] = organizationService.find("Viva con Agua Wasser GmbH").flatMap {
      case Some(orga) => {
        userService.profileListByRole(orga.publicId, "executive").flatMap {
          case Some(p) => Future.successful(views.html.imprints.imprintGMBH(orga, p))
          case _ => Future.successful(views.html.imprints.error("Vica con Agua Wasser GmbH"))
        } 
      }
      case _ => Future.successful(views.html.imprints.error("Vica con Agua Wasser GmbH"))
    }
    val vcaDataHtml:Future[Html] = organizationService.find("Herting Oberbeck Datenschutz GmbH").flatMap {
      case Some(orga) => Future.successful(views.html.imprints.imprintObDb(orga)) 
      case _ => Future.successful(views.html.imprints.error("Herting Oberbeck Datenschutz GmbH"))
    }    

    vcaSPHtml.flatMap(vca => vcaWaterHtml.flatMap(gmbh => vcaDataHtml.flatMap(data =>
      Future.successful(Ok(dispenserService.getTemplate(views.html.imprints.imprintBase(List(vca, gmbh, data)))))
      )))
    //Future.successful(Ok(dispenserService.getTemplate(views.html.imprints.imprintBase(List(vcaSPHtml, vcaWaterHtml, vcaDataHtml)))))
  }
}
