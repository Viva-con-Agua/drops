package oauth2server

import scalaoauth2.provider._

/**
  * Created by johann on 25.11.16.
  */
class DropsTokenEndpoint extends TokenEndpoint {
  val passwordNoCred = new Password() {
    override def clientCredentialRequired = false
  }

  override val handlers = Map(
    OAuthGrantType.AUTHORIZATION_CODE -> new AuthorizationCode(),
    OAuthGrantType.REFRESH_TOKEN -> new RefreshToken(),
    OAuthGrantType.CLIENT_CREDENTIALS -> new ClientCredentials(),
    OAuthGrantType.PASSWORD -> passwordNoCred, //new Password(),
    OAuthGrantType.IMPLICIT -> new Implicit()
  )
}
