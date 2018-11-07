package io.ceratech.fcm

import com.softwaremill.sttp.testing.SttpBackendStub
import com.softwaremill.sttp.{StringBody, SttpBackend}
import com.typesafe.config.ConfigFactory
import io.circe.syntax._
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncWordSpec, EitherValues, Matchers}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Tests for the FCM sender
  *
  * @author dries
  */
class FcmSenderSpec extends AsyncWordSpec with Matchers with AsyncMockFactory with EitherValues {

  import FcmJsonFormats._

  private lazy val config = ConfigFactory.load("application.test")

  private class TestFcmConfigProvider(backend: SttpBackend[Future, Nothing]) extends DefaultFcmConfigProvider(config) {
    override def constructBackend: SttpBackend[Future, Nothing] = backend
  }

  implicit val exectutionContext: ExecutionContext = ExecutionContext.global

  "the FcmSender" when {
    "sendNotification with valid configuration" should {
      "send a notification through FCM with a successfull response" in {
        val tokenRepository: TokenRepository = stub[TokenRepository]
        val backend = SttpBackendStub.asynchronousFuture
          .whenAnyRequest
          .thenRespond(successfullResponse)

        val fcmSender = new FcmSender(new TestFcmConfigProvider(backend), tokenRepository)

        val token = "123ab"
        val notification = FcmNotification(body = Some("body"))

        fcmSender.sendNotification(notification, token).map { result ⇒
          (tokenRepository.deleteToken _).verify(*).never()
          (tokenRepository.updateToken _).verify(*, *).never()
          result shouldBe true
        }
      }

      "send a correct notification body with a single token" in {
        val token = "123ab"

        val tokenRepository: TokenRepository = mock[TokenRepository]
        val backend = SttpBackendStub.asynchronousFuture
          .whenRequestMatches { req ⇒
            val json = io.circe.parser.parse(req.body.asInstanceOf[StringBody].s).right.value.hcursor
            json.get[Seq[String]]("registration_ids").right.value shouldBe Seq(token)
            json.get[Boolean]("dry_run").right.value shouldBe true
            true
          }
          .thenRespond(successfullResponse)

        val fcmSender = new FcmSender(new TestFcmConfigProvider(backend), tokenRepository)

        val notification = FcmNotification(body = Some("body"), title = Some("title"), badge = Some("1"))

        fcmSender.sendNotification(notification, token).map { result ⇒
          result shouldBe true
        }
      }

      "send a notification body with more than one token" in {
        val tokens = "123ab" :: "4321ba" :: Nil
        val notification = FcmNotification(body = Some("body"))

        val tokenRepository: TokenRepository = mock[TokenRepository]
        val backend = SttpBackendStub.asynchronousFuture
          .whenRequestMatches { req ⇒
            val json = io.circe.parser.parse(req.body.asInstanceOf[StringBody].s).right.value.hcursor
            json.get[Seq[String]]("registration_ids").right.value shouldBe tokens
            true
          }
          .thenRespond(successfullResponse)

        val fcmSender = new FcmSender(new TestFcmConfigProvider(backend), tokenRepository)

        fcmSender.sendNotification(notification, tokens).map { result ⇒
          result shouldBe true
        }
      }
    }

    "sendNotification with invalid tokens" should {
      "remove the token when FCM gives a invalid registration error" in {
        val token = "not-valid-anymore"

        val tokenRepository: TokenRepository = mock[TokenRepository]
        val backend = SttpBackendStub.asynchronousFuture
          .whenAnyRequest
          .thenRespond(failedResponse(FcmErrors.invalidRegistration, token))

        val fcmSender = new FcmSender(new TestFcmConfigProvider(backend), tokenRepository)

        (tokenRepository.deleteToken _).expects(token).returns(Future.successful(()))

        fcmSender.sendNotification(FcmNotification(body = Some("Test")), token).map { result ⇒
          result shouldBe false
        }
      }

      "remove the token when FCM gives a unregistered device error" in {
        val token = "unregistered"

        val tokenRepository: TokenRepository = mock[TokenRepository]
        val backend = SttpBackendStub.asynchronousFuture
          .whenAnyRequest
          .thenRespond(failedResponse(FcmErrors.unregisteredDevice, token))

        val fcmSender = new FcmSender(new TestFcmConfigProvider(backend), tokenRepository)

        (tokenRepository.deleteToken _).expects(token).returns(Future.successful(()))

        fcmSender.sendNotification(FcmNotification(body = Some("Test")), token).map { result ⇒
          result shouldBe false
        }
      }
    }

    "sendNotification with outdated tokens" should {
      "update the outdated token to the updated token" in {
        val outdatedToken = "outdated"
        val updatedToken = "updated"

        val tokenRepository: TokenRepository = mock[TokenRepository]
        val backend = SttpBackendStub.asynchronousFuture
          .whenAnyRequest
          .thenRespond(successfullUpdatedResponse(outdatedToken, updatedToken))

        val fcmSender = new FcmSender(new TestFcmConfigProvider(backend), tokenRepository)

        (tokenRepository.updateToken _).expects(outdatedToken, updatedToken).returns(Future.successful(()))

        fcmSender.sendNotification(FcmNotification(body = Some("123")), outdatedToken).map { result ⇒
          result shouldBe true
        }
      }
    }

    "sendToken should print the error if FCM gives an incorrect formatted JSON response" in {
      val token = "123ab"

      val tokenRepository: TokenRepository = mock[TokenRepository]
      val backend = SttpBackendStub.asynchronousFuture
        .whenAnyRequest
        .thenRespond("Non JSON response")

      val fcmSender = new FcmSender(new TestFcmConfigProvider(backend), tokenRepository)

      recoverToSucceededIf[FcmException] {
        fcmSender.sendNotification(FcmNotification(body = Some("123")), token)
      }
    }

    "sendToken should print the error if FCM gives another unhandlable error in the response" in {
      val token = "token"

      val tokenRepository: TokenRepository = mock[TokenRepository]
      val backend = SttpBackendStub.asynchronousFuture
        .whenAnyRequest
        .thenRespond(failedResponse("Unkown error", token))

      val fcmSender = new FcmSender(new TestFcmConfigProvider(backend), tokenRepository)

      (tokenRepository.deleteToken _).expects(token).never()
      (tokenRepository.updateToken _).expects(*, *).never()

      fcmSender.sendNotification(FcmNotification(body = Some("Test")), token).map { result ⇒
        result shouldBe false
      }
    }
  }

  private val successfullResponse = FcmResponse(1234, 1, 0, 0, FcmResult(Some("a"), None, None) :: Nil).asJson.toString()

  private def failedResponse(error: String, token: String) = FcmResponse(1234, 0, 1, 0, FcmResult(Some(token), None, Some(error)) :: Nil).asJson.toString()

  private def successfullUpdatedResponse(outdatedToken: String, updatedToken: String) = FcmResponse(1234, 1, 0, 1, FcmResult(Some(outdatedToken), Some(updatedToken), None) :: Nil).asJson.toString()
}
