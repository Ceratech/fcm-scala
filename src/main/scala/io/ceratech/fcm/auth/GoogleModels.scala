package io.ceratech.fcm.auth

import java.security.spec.PKCS8EncodedKeySpec
import java.security.{KeyFactory, PrivateKey}
import java.util.Base64

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

/**
  * Google key JSON data
  *
  * @author dries
  */
case class GoogleCredential(private_key: String, client_email: String) {

  /**
    * Decodes the private key (PKCS 8 encoded)
    */
  lazy val privateKey: PrivateKey = {
    val plainKey = private_key.split("\n").drop(1).dropRight(1).mkString
    val bytes = Base64.getDecoder.decode(plainKey)
    val privSpec = new PKCS8EncodedKeySpec(bytes)

    val keyFactory = KeyFactory.getInstance("RSA")
    keyFactory.generatePrivate(privSpec)
  }
}

/**
  * Access token; result of token call
  *
  * @author dries
  */
case class GoogleToken(access_token: String, token_type: String, expires_in: Long) {
  lazy val authHeader = s"$token_type $access_token"
}

object GoogleJsonFormats {
  implicit val googleCredentialDecoder: Decoder[GoogleCredential] = deriveDecoder[GoogleCredential]
  implicit val googleTokenDecoder: Decoder[GoogleToken] = deriveDecoder[GoogleToken]
  implicit val googleTokenEncoder: Encoder[GoogleToken] = deriveEncoder[GoogleToken]
}