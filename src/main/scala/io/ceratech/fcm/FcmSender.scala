package io.ceratech.fcm

import com.softwaremill.sttp._
import com.softwaremill.sttp.circe._
import com.typesafe.scalalogging.Logger
import io.circe.Error
import io.circe.syntax._
import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

/**
  * Sends notifications through the FCM API and handles the responses
  *
  * @author dries
  */
class FcmSender @Inject()(val fcmConfigProvider: FcmConfigProvider, val tokenRepository: TokenRepository)(implicit ec: ExecutionContext) {

  import FcmJsonFormats._

  private implicit val backend: SttpBackend[Future, Nothing] = fcmConfigProvider.constructBackend

  private val logger: Logger = Logger(classOf[FcmSender])

  private lazy val fcmConfig: FcmConfig = fcmConfigProvider.config

  def sendNotification(notification: FcmNotification, token: String): Future[Boolean] = {
    sendNotification(notification, token :: Nil)
  }

  def sendNotification(notification: FcmNotification, tokens: Seq[String]): Future[Boolean] = {
    val body = buildNotification(notification, tokens)

    val call = sttp.headers("Authorization" → s"Bearer ${fcmConfig.endpoint}")
      .body(body)
      .post(uri"${fcmConfig.endpoint}")
      .response(asJson[FcmResponse])
      .send()
      .zip(Future.successful(tokens))


    call.flatMap { case (response, originalTokens) ⇒
      handleFcmResponse(response, originalTokens)
    }
  }

  def handleFcmResponse(response: Response[Either[DeserializationError[Error], FcmResponse]], origTokens: Seq[String]): Future[Boolean] = {
    response.body match {
      case Right(body) ⇒ body match {
        case Right(obj) ⇒
          val result = obj.failure == 0
          if (obj.success == origTokens.size) {
            logger.debug(s"All (${obj.success} push notifications sent successfully")
          }

          Future.sequence(obj.results.zip(origTokens).map {
            case (res, origToken) ⇒
              val updateToken = res.registration_id.map { newToken ⇒
                tokenRepository.updateToken(origToken, newToken)
              }.getOrElse(Future.successful(()))

              val deleteToken = res.error.map {
                case FcmErrors.invalidRegistration | FcmErrors.unregisteredDevice ⇒
                  logger.debug(s"Invalid/unknown registration token $origToken, removing token")
                  tokenRepository.deleteToken(origToken)
                case err ⇒
                  logger.debug(s"Error sending push notifications: $err (messageId: ${res.message_id.getOrElse("<unkown>")})")
                  Future.successful(())
              }.getOrElse(Future.successful(())).map(_ ⇒ ())

              updateToken.zip(deleteToken).map(_ ⇒ ())
          }).map(_ ⇒ result)
        case Left(jsonError) ⇒
          Future.failed(FcmException(jsonError.message))
      }
      case Left(err) ⇒ Future.failed(FcmException(err))
    }
  }

  private def buildNotification(notification: FcmNotification, tokens: Seq[String]) = FcmMessage(tokens, fcmConfig.dryRun, notification).asJson
}
