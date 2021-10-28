package io.ceratech.fcm.auth

import com.typesafe.scalalogging.Logger
import io.ceratech.fcm.auth.GoogleJsonFormats._
import io.ceratech.fcm.{FcmConfig, FcmConfigProvider, FcmSender}
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim, JwtHeader}
import sttp.client3._
import sttp.client3.circe._

import java.time.{Duration, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
  * Handles authentication with Firebase for making authorized API calls
  */
trait FirebaseAuthenticator {
  /**
    * Gets the access token to use for FCM calls
    *
    * @return the result of fechting of the access token  call
    */
  def token: Future[Option[GoogleToken]]
}

/**
  * Handles, token, authentication with Firebase
  *
  * @author dries
  */
@Singleton
class DefaultFirebaseAuthenticator @Inject()(val fcmConfigProvider: FcmConfigProvider)(implicit ec: ExecutionContext) extends FirebaseAuthenticator {

  private val Scope = "https://www.googleapis.com/auth/firebase.messaging"
  private val GrantType = "urn:ietf:params:oauth:grant-type:jwt-bearer"

  private val backend: SttpBackend[Future, Any] = fcmConfigProvider.constructBackend

  private val logger: Logger = Logger(classOf[FcmSender])

  private lazy val fcmConfig: FcmConfig = fcmConfigProvider.config

  // After we fetch the token for the 1st time it's usuable for a period of time before it needs to be
  // fetched again. This vars cache the value and determine when it expires and thus needs to be refetched
  private var expires: Instant = Instant.now()
  private var cachedToken: Option[GoogleToken] = None

  override def token: Future[Option[GoogleToken]] = {
    if (cachedToken.isEmpty || Instant.now().isAfter(expires)) {
      val time = Instant.now()
      val assertion = createAssertion(time)

      basicRequest
        .body("grant_type" -> GrantType, "assertion" -> assertion)
        .post(uri"${fcmConfig.tokenEndpoint}")
        .response(asJson[GoogleToken])
        .send(backend)
        .map { response =>
          response.body match {
            case Right(obj) =>
              cachedToken = Some(obj)
              expires = time.plusSeconds(obj.expires_in)
              cachedToken
            case Left(err) =>
              logger.error(s"Error getting access token: $err")
              None
          }
        }
    } else {
      Future.successful(cachedToken)
    }
  }

  def createAssertion(time: Instant): String = {
    val jwt = JwtClaim(
      issuer = Some(fcmConfig.googleCredential.client_email),
      issuedAt = Some(time.getEpochSecond),
      expiration = Some(time.plus(Duration.ofMinutes(10)).getEpochSecond)
    ) + ("scope", Scope) + ("aud", fcmConfig.tokenEndpoint)

    val header = JwtHeader(JwtAlgorithm.RS256)
    JwtCirce.encode(header, jwt, fcmConfig.googleCredential.privateKey)
  }
}
