package api

import javax.inject.Inject

import api.query._
import daos.{OauthClientDao, UserDao}
import models.OauthClient
import play.api.libs.json._
import play.api.mvc.Request
import play.api.mvc.AnyContent

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


class ApiRequestProvider @Inject() (
  oauthClientDao : OauthClientDao,
  userDao: UserDao
) {
  def get[A](request: Request[A]) = ApiRequest[A](request, oauthClientDao, userDao)
}
/**
  * Created by johann on 21.12.16.
  */
case class ApiRequest[A](request : Request[A], oauthClientDao : OauthClientDao, userDao: UserDao){
  val cĺientId = request.queryString("client_id").headOption.getOrElse(
    throw new Exception // Todo: Meaningful Exception
  )
  val clientSecret = request.queryString("client_secret").headOption.getOrElse(
    throw new Exception // Todo: Meaningful Exception
  )
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

  def getClient : Future[Option[OauthClient]] =
    oauthClientDao.find(cĺientId, clientSecret)
}
