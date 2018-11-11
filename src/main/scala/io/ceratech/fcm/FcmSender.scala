package io.ceratech.fcm

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import com.typesafe.scalalogging.Logger
import io.ceratech.fcm.auth.FirebaseAuthenticator
import io.circe.Error
import io.circe.parser._
import io.circe.syntax._
import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

/**
  * Sends notifications through the FCM API and handles the responses
  *
  * @author dries
  */
class FcmSender @Inject()(val fcmConfigProvider: FcmConfigProvider, val tokenRepository: TokenRepository, val firebaseAuthenticator: FirebaseAuthenticator)(implicit ec: ExecutionContext) {

  import FcmJsonFormats._

  private implicit val backend: SttpBackend[Future, Nothing] = fcmConfigProvider.constructBackend

  private val logger: Logger = Logger(classOf[FcmSender])

  private lazy val fcmConfig: FcmConfig = fcmConfigProvider.config

  /**
    * Send a message through FCM
    *
    * @param message the mesasge to send
    * @return the name assigned by FCM as result of sending the message
    */
  def sendMessage(message: FcmMessage): Future[Option[String]] = {
    val body = buildBody(message)

    val call = firebaseAuthenticator.token.map {
      case Some(token) ⇒ token
      case _ ⇒ throw FcmException("No Google token to make FCM calls")
    }.flatMap { token ⇒
      sttp.headers("Authorization" → token.authHeader)
        .body(body)
        .post(uri"${fcmConfig.endpoint}")
        .response(asJson[FcmResponse])
        .send()
    }

    call.flatMap {
      handleFcmResponse(_, message)
    }
  }

  def handleFcmResponse(response: Response[Either[DeserializationError[Error], FcmResponse]], origMessage: FcmMessage): Future[Option[String]] = {
    response.body match {
      case Right(body) ⇒ body match {
        case Right(obj) ⇒
          logger.debug(s"FCM message sent successfully")
          Future.successful(Some(obj.name))
        case Left(jsonError) ⇒
          logger.error(s"Error while decoding FCM JSON response: ${jsonError.message}")
          Future.failed(FcmException(jsonError.message))
      }
      case Left(err) ⇒
        decode[FcmError](err) match {
          case Right(error) if FcmErrors.InvalidTokens.contains(error.error_code) ⇒
            origMessage.target match {
              case FcmTokenTarget(token) ⇒
                logger.debug(s"Token (no longer) valid: '$token', deleting")
                tokenRepository.deleteToken(token).map(_ ⇒ None)
              case _ ⇒ Future.successful(None)
            }
          case _ ⇒
            logger.debug(s"FCM error response: $err")
            Future.failed(FcmException(err))

        }
    }
  }

  private def buildBody(message: FcmMessage) = FcmBody(fcmConfig.validateOnly, message).asJson
}
