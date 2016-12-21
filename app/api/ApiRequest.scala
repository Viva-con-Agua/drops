package api

import javax.inject.Inject

import daos.OauthClientDao
import models.OauthClient
import play.api.mvc.Request

import scala.concurrent.Future


class ApiRequestProvider @Inject() (
  oauthClientDao : OauthClientDao
) {
  def get[A](request: Request[A]) = ApiRequest[A](request, oauthClientDao)
}
/**
  * Created by johann on 21.12.16.
  */
case class ApiRequest[A](request : Request[A], oauthClientDao : OauthClientDao){
  val cĺientId = request.queryString("client_id").headOption.getOrElse(
    throw new Exception // Todo: Meaningful Exception
  )
  val clientSecret = request.queryString("client_secret").headOption.getOrElse(
    throw new Exception // Todo: Meaningful Exception
  )

  def getClient : Future[Option[OauthClient]] =
    oauthClientDao.find(cĺientId, clientSecret)
}
