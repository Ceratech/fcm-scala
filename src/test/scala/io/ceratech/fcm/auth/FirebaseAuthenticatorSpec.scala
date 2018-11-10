package io.ceratech.fcm.auth

import java.time.{Clock, Instant, ZoneId}

import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import com.softwaremill.sttp.testing.SttpBackendStub
import com.softwaremill.sttp.{Response, SttpBackend}
import com.typesafe.config.ConfigFactory
import io.ceratech.fcm.DefaultFcmConfigProvider
import io.ceratech.fcm.auth.GoogleJsonFormats._
import io.circe.syntax._
import org.scalatest.tagobjects.{Network, Slow}
import org.scalatest.{AsyncWordSpec, Matchers, OptionValues}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Firebase authenticator tests
  *
  * @author dries
  */
class FirebaseAuthenticatorSpec extends AsyncWordSpec with Matchers with OptionValues {

  private lazy val config = ConfigFactory.load("application.test")

  private class TestFcmConfigProvider(backend: SttpBackend[Future, Nothing]) extends DefaultFcmConfigProvider(config) {
    override def constructBackend: SttpBackend[Future, Nothing] = backend
  }

  implicit val exectutionContext: ExecutionContext = ExecutionContext.global

  "The FirebaseAuthenticator" when {

    "token" should {
      "fetch a token if there is not cached token" taggedAs(Network, Slow) in {
        val backend = AsyncHttpClientFutureBackend()
        val configProvider = new TestFcmConfigProvider(backend)
        val authenticator = new FirebaseAuthenticator(configProvider)

        authenticator.token.map { maybeToken ⇒
          maybeToken should not be empty
          maybeToken.value.token_type shouldBe "Bearer"
        }
      }

      "return the token from cache" in {
        val googleToken = GoogleToken("token", "Bearer", 3600L)

        var hit = false
        val backend = SttpBackendStub.asynchronousFuture
          .whenRequestMatchesPartial {
            case _ if !hit ⇒
              hit = true
              Response.ok(googleToken.asJson.toString())
            case _ ⇒
              Response.error("FAIL", 400)
          }


        val configProvider = new TestFcmConfigProvider(backend)
        val authenticator = new FirebaseAuthenticator(configProvider)

        // Call 2 times; if the cache isn't used the second actual call to the mock server gives an 400; effectively returning a [[None]]
        authenticator.token.flatMap { _ ⇒
          authenticator.token
        }.map { token ⇒
          token should not be empty
        }
      }

      "give a None when the response is not a 200" in {
        val backend = SttpBackendStub.asynchronousFuture
          .whenAnyRequest
          .thenRespondWithCode(400)

        val configProvider = new TestFcmConfigProvider(backend)
        val authenticator = new FirebaseAuthenticator(configProvider)

        authenticator.token.map { token ⇒
          token shouldBe empty
        }
      }

      "give a None when the response body is not parsable as a GoogleToken" in {
        val backend = SttpBackendStub.asynchronousFuture
          .whenAnyRequest
          .thenRespond("No JSON")

        val configProvider = new TestFcmConfigProvider(backend)
        val authenticator = new FirebaseAuthenticator(configProvider)

        authenticator.token.map { token ⇒
          token shouldBe empty
        }
      }
    }

    "createAssertion" should {
      "create a signed assertion if provided with a valid key" in {
        val time = Instant.now()

        val backend = SttpBackendStub.asynchronousFuture
        val configProvider = new TestFcmConfigProvider(backend)
        val authenticator = new FirebaseAuthenticator(configProvider)

        val assertion = authenticator.createAssertion(time)
        assertion.length should be > 0
      }
    }
  }
}
