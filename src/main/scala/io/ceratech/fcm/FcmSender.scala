package io.ceratech.fcm

import javax.inject.Inject

import play.api.Logger
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Sends notifications through the FCM API and handles the responses
  *
  * @author dries
  */
class FcmSender @Inject()(val fcmConfigProvider: FcmConfigProvider, val wsClient: WSClient, val tokenRepository: TokenRepository)(implicit ec: ExecutionContext) {

  import FcmJsonFormats._

  private val logger: Logger = Logger(classOf[FcmSender])

  private lazy val fcmConfig: FcmConfig = fcmConfigProvider.config

  def sendNotification(notification: FcmNotification, token: String): Future[Boolean] = {
    sendNotification(notification, token :: Nil)
  }

  def sendNotification(notification: FcmNotification, tokens: Seq[String]): Future[Boolean] = {
    val body = buildNotification(notification, tokens)

    val call = wsClient.url(fcmConfig.endpoint)
      .withHttpHeaders(("Authorization", s"key=${fcmConfig.key}"))
      .post(body).zip(Future.successful(tokens))

    call.flatMap { case (response, originalTokens) ⇒
      handleFcmResponse(response, originalTokens)
    }
  }

  def handleFcmResponse(response: WSResponse, origTokens: Seq[String]): Future[Boolean] = {
    response.json.validate[FcmResponse] match {
      case JsSuccess(obj, _) ⇒
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

      case JsError(errors) ⇒ Future.failed(FcmException(errors.map {
        case (path, validationErrors) ⇒ path.toString() + ": " + validationErrors.map(_.message).mkString(", ")
      }.mkString("Validation errors: ", ", ", "")))
    }
  }

  private def buildNotification(notification: FcmNotification, tokens: Seq[String]) = {
    Json.obj(
      "registration_ids" → tokens,
      "dry_run" → fcmConfig.dryRun,
      "notification" → Json.toJson(notification)
    )
  }
}
