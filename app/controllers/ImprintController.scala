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
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import models._
class ImprintController @Inject()(
  val messagesApi: MessagesApi,
  val env:Environment[User, CookieAuthenticator],
  dispenserService: DispenserService,
  organizationService: OrganizationService,
  userService: UserService
)extends Silhouette[User, CookieAuthenticator]{
  
  

  def imprint = Action.async { implicit request =>
    val vcaOrga = organizationService.withBankaccounts("Viva con Agua de Sankt Pauli e.V.")
    val vcaWater = organizationService.find("Viva con Agua Wasser GmbH")
    val executive = organizationService.withProfileByRole(vcaOrga.publicId, "executive").flatMap {
      case Some(orga) => orga.profile.head
      case _ => Future.successful("No executive found")
    }
    val htmlList:List[Html] = Nil
    htmlList.append(views.html.imprints.imprintEV())
    Future.successful(Ok(dispenserService.getTemplate(views.html.imprints.imprintBase(vcaOrga, executive, profiles, visdp, vcaOrga.bankaccount))))
  }


}
