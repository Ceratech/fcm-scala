package io.ceratech.fcm

import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.testing.SttpBackendStub
import com.typesafe.config.ConfigFactory
import io.ceratech.fcm.auth.{FirebaseAuthenticator, GoogleToken}
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncWordSpec, EitherValues, Matchers}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Tests for the FCM sender
  *
  * @author dries
  */
class DefaultFcmSenderSpec extends AsyncWordSpec with Matchers with AsyncMockFactory with EitherValues {

  private lazy val config = ConfigFactory.load("application.test")

  private class TestFcmConfigProvider(backend: SttpBackend[Future, Nothing]) extends DefaultFcmConfigProvider(config) {
    override def constructBackend: SttpBackend[Future, Nothing] = backend
  }

  implicit val exectutionContext: ExecutionContext = ExecutionContext.global

  "the FcmSender" when {
    "sendNotification with valid configuration" should {
      "send a notification through FCM with a successfull response" in {
        val tokenRepository: TokenRepository = stub[TokenRepository]
        val firebaseAuthenticator: FirebaseAuthenticator = mock[FirebaseAuthenticator]
        val backend = SttpBackendStub.asynchronousFuture
          .whenAnyRequest
          .thenRespond(successfullResponse)

        val fcmSender = new DefaultFcmSender(new TestFcmConfigProvider(backend), tokenRepository, firebaseAuthenticator)

        val token = "123ab"
        val message = FcmMessage(FcmNotification(body = Some("body")), FcmTokenTarget(token))

        (firebaseAuthenticator.token _).expects().returns(Future.successful(Some(GoogleToken("token", "Bearer", 3600))))

        fcmSender.sendMessage(message).map { result ⇒
          (tokenRepository.deleteToken _).verify(*).never()
          result shouldBe Some("message-id-from-fcm")
        }
      }
    }

    "sendNotification with invalid tokens" should {
      "remove the token when FCM gives a unregistered token error" in {
        val token = "unregistered"

        val tokenRepository: TokenRepository = mock[TokenRepository]
        val firebaseAuthenticator: FirebaseAuthenticator = mock[FirebaseAuthenticator]
        val backend = SttpBackendStub.asynchronousFuture
          .whenAnyRequest
          .thenRespondWithCode(400, failedResponse(FcmErrors.Unregistered))

        val fcmSender = new DefaultFcmSender(new TestFcmConfigProvider(backend), tokenRepository, firebaseAuthenticator)

        (firebaseAuthenticator.token _).expects().returns(Future.successful(Some(GoogleToken("token", "Bearer", 3600))))
        (tokenRepository.deleteToken _).expects(token).returns(Future.successful(()))

        fcmSender.sendMessage(FcmMessage(FcmNotification(body = Some("Test")), FcmTokenTarget(token))).map { result ⇒
          result shouldBe None
        }
      }

      "remove the token when FCM gives a sender id mismatch error" in {
        val token = "sender-mismatch"

        val tokenRepository: TokenRepository = mock[TokenRepository]
        val firebaseAuthenticator: FirebaseAuthenticator = mock[FirebaseAuthenticator]
        val backend = SttpBackendStub.asynchronousFuture
          .whenAnyRequest
          .thenRespondWithCode(400, failedResponse(FcmErrors.SenderIdMismatch))

        val fcmSender = new DefaultFcmSender(new TestFcmConfigProvider(backend), tokenRepository, firebaseAuthenticator)

        (firebaseAuthenticator.token _).expects().returns(Future.successful(Some(GoogleToken("token", "Bearer", 3600))))
        (tokenRepository.deleteToken _).expects(token).returns(Future.successful(()))

        fcmSender.sendMessage(FcmMessage(FcmNotification(body = Some("Test")), FcmTokenTarget(token))).map { result ⇒
          result shouldBe None
        }
      }
    }

    "sendToken should print the error if FCM gives an incorrect formatted JSON response" in {
      val token = "123ab"

      val tokenRepository: TokenRepository = mock[TokenRepository]
      val firebaseAuthenticator: FirebaseAuthenticator = mock[FirebaseAuthenticator]
      val backend = SttpBackendStub.asynchronousFuture
        .whenAnyRequest
        .thenRespond("Non JSON response")

      val fcmSender = new DefaultFcmSender(new TestFcmConfigProvider(backend), tokenRepository, firebaseAuthenticator)

      (firebaseAuthenticator.token _).expects().returns(Future.successful(Some(GoogleToken("token", "Bearer", 3600))))

      recoverToSucceededIf[FcmException] {
        fcmSender.sendMessage(FcmMessage(FcmNotification(body = Some("123")), FcmTokenTarget(token)))
      }
    }

    "sendToken should print the error if FCM gives an unexpected response code" in {
      val token = "123ab"

      val tokenRepository: TokenRepository = mock[TokenRepository]
      val firebaseAuthenticator: FirebaseAuthenticator = mock[FirebaseAuthenticator]
      val backend = SttpBackendStub.asynchronousFuture
        .whenAnyRequest
        .thenRespondWithCode(400)

      val fcmSender = new DefaultFcmSender(new TestFcmConfigProvider(backend), tokenRepository, firebaseAuthenticator)

      (firebaseAuthenticator.token _).expects().returns(Future.successful(Some(GoogleToken("token", "Bearer", 3600))))

      recoverToSucceededIf[FcmException] {
        fcmSender.sendMessage(FcmMessage(FcmNotification(body = Some("123")), FcmTokenTarget(token)))
      }
    }

    "sendToken should print the error if FCM gives another unhandlable error in the response" in {
      val token = "token"

      val tokenRepository: TokenRepository = mock[TokenRepository]
      val firebaseAuthenticator: FirebaseAuthenticator = mock[FirebaseAuthenticator]
      val backend = SttpBackendStub.asynchronousFuture
        .whenAnyRequest
        .thenRespond(failedResponse("Unkown error"))

      val fcmSender = new DefaultFcmSender(new TestFcmConfigProvider(backend), tokenRepository, firebaseAuthenticator)

      (firebaseAuthenticator.token _).expects().returns(Future.successful(Some(GoogleToken("token", "Bearer", 3600))))
      (tokenRepository.deleteToken _).expects(token).never()

      recoverToSucceededIf[FcmException] {
        fcmSender.sendMessage(FcmMessage(FcmNotification(body = Some("123")), FcmTokenTarget(token)))
      }
    }

    "sendToken should not call the FCM API if not auth token can be obtained" in {
      val token = "123ab"

      val tokenRepository: TokenRepository = mock[TokenRepository]
      val firebaseAuthenticator: FirebaseAuthenticator = mock[FirebaseAuthenticator]
      val backend = SttpBackendStub.asynchronousFuture
        .whenRequestMatches(_ ⇒ fail("No request should be made"))
        .thenRespondOk()

      val fcmSender = new DefaultFcmSender(new TestFcmConfigProvider(backend), tokenRepository, firebaseAuthenticator)

      (firebaseAuthenticator.token _).expects().returns(Future.successful(None))

      recoverToSucceededIf[FcmException] {
        fcmSender.sendMessage(FcmMessage(FcmNotification(body = Some("123")), FcmTokenTarget(token)))
      }
    }
  }

  private val successfullResponse = """{"name":"message-id-from-fcm"}"""

  private def failedResponse(error: String) = s"""{"error_code": "$error"}"""
}
