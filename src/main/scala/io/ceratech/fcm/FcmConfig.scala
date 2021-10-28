package io.ceratech.fcm

import io.ceratech.fcm.auth.GoogleCredential
import io.circe.parser._

import scala.io.Source

/**
  * FCM configuration
  *
  * @author dries
  */
case class FcmConfig(endpoint: String, keyFile: String, validateOnly: Boolean, tokenEndpoint: String = "https://www.googleapis.com/oauth2/v4/token") {

  import io.ceratech.fcm.auth.GoogleJsonFormats._

  lazy val googleCredential: GoogleCredential = {
    decode[GoogleCredential](Source.fromFile(keyFile).mkString) match {
      case Right(obj) => obj
      case _ => throw new IllegalStateException("Invalid keyFile specified")
    }
  }
}
