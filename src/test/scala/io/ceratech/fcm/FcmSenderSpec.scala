package io.ceratech.fcm

import com.typesafe.config.ConfigFactory
import io.ceratech.fcm.helpers.JsObjectMatcher.jsMatches
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{AsyncWordSpec, Matchers}
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.ws.{BodyWritable, WSClient, WSRequest, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Tests for the FCM sender
  *
  * @author dries
  */
class FcmSenderSpec extends AsyncWordSpec with Matchers with MockitoSugar {

  private lazy val config = Configuration(ConfigFactory.load("application.test"))

  implicit val exectutionContext: ExecutionContext = ExecutionContext.global

  private def mocks = new {
    val tokenRepository: TokenRepository = mock[TokenRepository]
    val wsClient: WSClient = mock[WSClient]

    // Default WS Client setup
    val mockedRequest: WSRequest = mock[WSRequest]
    when(wsClient.url(anyString())) thenReturn mockedRequest
    when(mockedRequest.withHttpHeaders(any[(String, String)])) thenReturn mockedRequest

    // Sender
    val fcmSender = new FcmSender(config, wsClient, tokenRepository)
  }

  "the FcmSender" when {
    "sendNotification with valid configuration" should {
      "send a notification through FCM with a successfull response" in {
        val m = mocks
        import m._

        val token = "123ab"
        val notification = FcmNotification(body = Some("body"))

        val response = mock[WSResponse]
        when(response.json) thenReturn successfullResponse

        when(mockedRequest.post(any[JsObject])(any[BodyWritable[JsObject]])) thenReturn Future.successful(response)

        fcmSender.sendNotification(notification, token).map { result ⇒
          verifyZeroInteractions(tokenRepository)
          result shouldBe true
        }
      }

      "send a correct notification body with a single token" in {
        val m = mocks
        import m._

        val token = "123ab"
        val notification = FcmNotification(body = Some("body"), title = Some("title"), badge = Some("1"))

        val response = mock[WSResponse]
        when(response.json) thenReturn successfullResponse

        when(mockedRequest.post(jsMatches { json ⇒
          // Check whole JSON post body
          json("registration_ids") should be (Json.arr(token))
          json("dry_run") should be (JsBoolean(true))

          val body = json("notification")
          body shouldBe a [JsObject]

          body("body") should be (JsString(notification.body.get))
          body("title") should be (JsString(notification.title.get))
          body("badge") should be (JsString(notification.badge.get))
        })(any[BodyWritable[JsObject]])) thenReturn Future.successful(response)

        fcmSender.sendNotification(notification, token).map { result ⇒
          result shouldBe true
        }
      }

      "send a notification body with more than one token" in {
        val m = mocks
        import m._

        val tokens = "123ab" :: "4321ba" :: Nil
        val notification = FcmNotification(body = Some("body"))

        val response = mock[WSResponse]
        when(response.json) thenReturn successfullResponse

        when(mockedRequest.post(jsMatches { json ⇒
          json("registration_ids") should be (JsArray(tokens.map(JsString)))
        })(any[BodyWritable[JsObject]])) thenReturn Future.successful(response)

        fcmSender.sendNotification(notification, tokens).map { result ⇒
          result shouldBe true
        }
      }
    }

    "sendNotification with invalid tokens" should {
      "remove the token when FCM gives a invalid registration error" in {
        val m = mocks
        import m._

        val token = "not-valid-anymore"

        val response = mock[WSResponse]
        when(response.json) thenReturn failedResponse(FcmErrors.invalidRegistration, token)

        when(mockedRequest.post(any[JsObject])(any[BodyWritable[JsObject]])) thenReturn Future.successful(response)
        when(tokenRepository.deleteToken(token)) thenReturn Future.successful(())

        fcmSender.sendNotification(FcmNotification(body = Some("Test")), token).map { result ⇒
          verify(tokenRepository).deleteToken(token)
          result shouldBe false
        }
      }

      "remove the token when FCM gives a unregistered device error" in {
        val m = mocks
        import m._

        val token = "unregistered"

        val response = mock[WSResponse]
        when(response.json) thenReturn failedResponse(FcmErrors.unregisteredDevice, token)

        when(mockedRequest.post(any[JsObject])(any[BodyWritable[JsObject]])) thenReturn Future.successful(response)
        when(tokenRepository.deleteToken(token)) thenReturn Future.successful(())

        fcmSender.sendNotification(FcmNotification(body = Some("Test")), token).map { result ⇒
          verify(tokenRepository).deleteToken(token)
          result shouldBe false
        }
      }
    }

    "sendNotification with outdated tokens" should {
      "update the outdated token to the updated token" in {
        val m = mocks
        import m._

        val outdatedToken = "outdated"
        val updatedToken = "updated"

        val response = mock[WSResponse]
        when(response.json) thenReturn successfullUpdatedResponse(outdatedToken, updatedToken)

        when(mockedRequest.post(any[JsObject])(any[BodyWritable[JsObject]])) thenReturn Future.successful(response)
        when(tokenRepository.updateToken(outdatedToken, updatedToken)) thenReturn Future.successful(())

        fcmSender.sendNotification(FcmNotification(body = Some("123")), outdatedToken).map { result ⇒
          verify(tokenRepository).updateToken(outdatedToken, updatedToken)
          result shouldBe true
        }
      }
    }

    "sendToken should print the error if FCM gives an incorrect formatted JSON response" in {
      val m = mocks
      import m._

      val token = "123ab"

      val response = mock[WSResponse]
      when(response.json) thenReturn JsString("Unkown error")

      when(mockedRequest.post(any[JsObject])(any[BodyWritable[JsObject]])) thenReturn Future.successful(response)

      recoverToSucceededIf[FcmException] {
        fcmSender.sendNotification(FcmNotification(body = Some("123")), token)
      }
    }

    "sendToken should print the error if FCM gives another unhandlable error in the response" in {
      val m = mocks
      import m._

      val token = "token"

      val response = mock[WSResponse]
      when(response.json) thenReturn failedResponse("Unknown error", token)

      when(mockedRequest.post(any[JsObject])(any[BodyWritable[JsObject]])) thenReturn Future.successful(response)
      when(tokenRepository.deleteToken(token)) thenReturn Future.successful(())

      fcmSender.sendNotification(FcmNotification(body = Some("Test")), token).map { result ⇒
        verifyZeroInteractions(tokenRepository)
        result shouldBe false
      }
    }
  }

  private val successfullResponse = Json.obj(
    "multicast_id" → 1234,
    "success" → 1,
    "failure" → 0,
    "canonical_ids" → 0,
    "results" → Json.arr(
      Json.obj(
        "message_id" → "a"
      )
    )
  )

  private def failedResponse(error: String, token: String): JsObject = Json.obj(
    "multicast_id" → 1234,
    "success" → 0,
    "failure" → 1,
    "canonical_ids" → 0,
    "results" → Json.arr(
      Json.obj(
        "message_id" → token,
        "error" → error
      )
    )
  )

  private def successfullUpdatedResponse(outdatedToken: String, updatedToken: String) = Json.obj(
    "multicast_id" → 1234,
    "success" → 1,
    "failure" → 0,
    "canonical_ids" → 1,
    "results" → Json.arr(
      Json.obj(
        "message_id" → outdatedToken,
        "registration_id" → updatedToken
      )
    )
  )
}
