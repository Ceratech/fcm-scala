package io.ceratech.fcm

import com.typesafe.scalalogging.Logger
import io.ceratech.fcm.auth.FirebaseAuthenticator
import io.circe.Error
import io.circe.syntax._
import sttp.client3._
import sttp.client3.circe._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/**
  * FCM functions
  */
trait FcmSender {
  /**
    * Send a message through FCM
    *
    * @param message the mesasge to send
    * @return the name assigned by FCM as result of sending the message
    */
  def sendMessage(message: FcmMessage): Future[Option[String]]
}

/**
  * Sends notifications through the FCM API and handles the responses
  *
  * @author dries
  */
@Singleton
class DefaultFcmSender @Inject()(val fcmConfigProvider: FcmConfigProvider, val tokenRepository: TokenRepository, val firebaseAuthenticator: FirebaseAuthenticator)(implicit ec: ExecutionContext) extends FcmSender {

  import FcmJsonFormats._

  private val backend: SttpBackend[Future, Any] = fcmConfigProvider.constructBackend

  private val logger: Logger = Logger(classOf[DefaultFcmSender])

  private lazy val fcmConfig: FcmConfig = fcmConfigProvider.config

  override def sendMessage(message: FcmMessage): Future[Option[String]] = {
    implicit val callSuccess: retry.Success[Any] = retry.Success.always
    val policy = retry.When {
      case NonFatal(e) if e.isInstanceOf[FcmException] => retry.Backoff(max = 3)
    }

    policy(() => runCall(message))
  }

  private def runCall(message: FcmMessage): Future[Option[String]] = {
    val body = buildBody(message)

    val call = firebaseAuthenticator.token.map {
      case Some(token) => token
      case _ => throw FcmException("No Google token to make FCM calls")
    }.flatMap { token =>
      basicRequest.header("Authorization", token.authHeader)
        .body(body)
        .post(uri"${fcmConfig.endpoint}")
        .response(asJsonEither[FcmErrorWrapper, FcmResponse])
        .send(backend)
    }

    call.flatMap {
      handleFcmResponse(_, message)
    }
  }

  def handleFcmResponse(response: Response[Either[ResponseException[FcmErrorWrapper, Error], FcmResponse]], origMessage: FcmMessage): Future[Option[String]] = {
    response.body match {
      case Right(obj) =>
        logger.debug(s"FCM message sent successfully")
        Future.successful(Some(obj.name))
      case Left(err) =>
        err match {
          case HttpError(body, _) =>
            if (FcmErrors.InvalidTokens.intersect(body.error.details.filter(_.errorCode.isDefined).map(_.errorCode.get).toSet).nonEmpty) {
              origMessage.target match {
                case FcmTokenTarget(token) =>
                  logger.debug(s"Token (no longer) valid: '$token', deleting")
                  tokenRepository.deleteToken(token).map(_ => None)
                case _ => Future.successful(None)
              }
            } else {
              logger.debug(s"FCM JSON error response: $body")
              Future.failed(FcmException(s"FCM JSON error, $body"))
            }
          case DeserializationException(body, error) =>
            logger.debug(s"FCM error response: $body")
            Future.failed(FcmException(s"FCM error, $body", error))
        }
    }
  }

  private def buildBody(message: FcmMessage) = FcmBody(fcmConfig.validateOnly, message).asJson
}
