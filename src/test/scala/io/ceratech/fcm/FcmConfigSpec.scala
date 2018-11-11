package io.ceratech.fcm

import java.io.FileNotFoundException

import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpec}

import scala.collection.JavaConverters._

/**
  * Config tests
  *
  * @author dries
  */
class FcmConfigSpec extends WordSpec with Matchers {

  "The FcmConfig" should {
    "load from a valid TypeSafe config" in {
      val provider = new DefaultFcmConfigProvider(ConfigFactory.load("application.test"))

      provider.config.googleCredential.private_key should include("-----BEGIN PRIVATE KEY-----")
      provider.config.googleCredential.privateKey.getAlgorithm shouldBe "RSA"
    }

    "give an error when the config has a non existant key file path" in {
      val configuration = ConfigFactory.parseMap(Map(
        "fcm.endpoint" → "endpoint",
        "fcm.validate-only" → true,
        "fcm.key-file" → "/doesnt/exist",
        "fcm.token-endpoint" → "endpoint"
      ).asJava)

      val provider = new DefaultFcmConfigProvider(configuration)

      an[FileNotFoundException] should be thrownBy provider.config.googleCredential
    }

    "give an error when the config has a non JSON key file path" in {
      val configuration = ConfigFactory.parseMap(Map(
        "fcm.endpoint" → "endpoint",
        "fcm.validate-only" → true,
        "fcm.key-file" → "src/test/resources/key-invalid.json",
        "fcm.token-endpoint" → "endpoint"
      ).asJava)

      val provider = new DefaultFcmConfigProvider(configuration)

      an[IllegalStateException] should be thrownBy provider.config.googleCredential
    }
  }
}
