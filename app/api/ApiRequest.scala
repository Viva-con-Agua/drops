package api

import javax.inject.Inject

import api.query._
import daos.{OauthClientDao, UserDao}
import models.OauthClient
import play.api.Configuration
import play.api.libs.json._
import play.api.mvc.Request
import play.api.mvc.AnyContent
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


class ApiRequestProvider @Inject() (
  configuration: Configuration,
  oauthClientDao : OauthClientDao,
  userDao: UserDao
) {
  def get[A](request: Request[A]) = ApiRequest[A](request, oauthClientDao, userDao, configuration)
}
/**
  * Created by johann on 21.12.16.
  */
case class ApiRequest[A](request : Request[A], oauthClientDao : OauthClientDao, userDao: UserDao, config: Configuration){
  val cĺientId = request.queryString("client_id").headOption.getOrElse(
    throw new Exception // Todo: Meaningful Exception
  )
  val clientSecret = request.queryString("client_secret").headOption
  val version = request.queryString.getOrElse("version",
    request.queryString.getOrElse("v",
      Seq("1.1.0") // change this, if a new version of the webservice was implemented (so it uses the new version by default)
    )
  ).headOption.getOrElse(
    throw new Exception // Todo: Meaningful Exception
  )

  val query = request.body match {
    case b : AnyContent => b.asJson match {
      case Some(json) => json == Json.obj() match {
        // check for an empty JSON
        case true => None
        case _ => (version match {
          case "1.0.0" => Json.fromJson[v1_0_0.ApiQuery](json)
          case "1.1.0" => Json.fromJson[v1_1_0.ApiQuery](json)
          case _ => throw new Exception("Given version number is unknown!")
            // add new versions here!
        }) match {
          case JsSuccess(query, path : JsPath) => Some(query)
          case e: JsError => throw new Exception(JsError.toJson(e).toString()) // Todo: Meaning is that the given JSON query is not a valid query
        }
      }
      case _ => None
    }
    case _ => None // all other possible contents are unknown to me
  }

  def getClient : Future[Either[OauthClient, Exception]] =
    config.getString("drops.ws.security").getOrElse("none") match {
      case "none" => oauthClientDao.find(cĺientId).map(_ match {
        case Some(client) => Left(client)
        case _ => Right(new Exception("rest.api.givenClientNotFound"))
      })
      case "secret" => clientSecret match {
        case Some(secret) => oauthClientDao.find(cĺientId, secret).map(_ match {
          case Some(client) => Left(client)
          case _ => Right(new Exception("rest.api.givenClientNotFound"))
        })
        case _ => Future.successful(Right(new Exception("rest.api.noClientSecretGiven")))
      }
      case "sluice" => {
        // Todo: Implement integration for using sluice in intra-microservice communication
        Future.successful(Right(new Exception("rest.api.securityMethodNotImplemented")))
      }
    }
}
