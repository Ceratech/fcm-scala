package io.ceratech.fcm

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

case class FcmBody(validate_only: Boolean, message: FcmMessage)

/**
  * Message that gets send to FCM and gets returned as response
  *
  * @author dries
  */
case class FcmMessage(notification: FcmNotification, target: FcmTarget, data: Map[String, String] = Map(), apns: Option[FcmApnsConfig] = None, android: Map[String, Json] = Map())

/**
  * FCM accepts different types of targets; they all have different keys; only 1 can be present at the same time
  */
trait FcmTarget {
  val target: String
  val value: String

  def asJson: (String, Json) = target → Json.fromString(value)
}

case class FcmTokenTarget(value: String) extends FcmTarget {
  override val target: String = "token"
}

case class FcmTopicTarget(value: String) extends FcmTarget {
  override val target: String = "topic"
}

case class FcmConditionTarget(value: String) extends FcmTarget {
  override val target: String = "condition"
}

/**
  * Notification
  */
case class FcmNotification(body: Option[String], title: Option[String] = None)

/**
  * APNS specific fields that can be sent trough FCM for iOS devices
  */
case class FcmApnsConfig(headers: Map[String, String], payload: Map[String, Json])

/**
  * FCM response object
  */
case class FcmResponse(name: String)

/**
  * FCM error response
  */
case class FcmError(code: Int, message: String, status: String, details: Seq[FcmErrorDetail])

/**
  * Details about an error thrown by FCM
  */
case class FcmErrorDetail(errorCode: String)

/**
  * Error wrapper
  */
case class FcmErrorWrapper(error: FcmError)

/**
  * JSON formats
  */
object FcmJsonFormats {
  implicit val fcmMessageEncoder: Encoder[FcmMessage] = (m: FcmMessage) => Json.obj(
    ("notification", m.notification.asJson),
    m.target.asJson,
    ("data", m.data.asJson),
    ("apns", m.apns.asJson),
    ("android", m.android.asJson)
  )

  implicit val fcmNotificationEncoder: Encoder[FcmNotification] = deriveEncoder[FcmNotification]
  implicit val fcmBodyEncoder: Encoder[FcmBody] = deriveEncoder[FcmBody]
  implicit val fcmApnsConfigEncoder: Encoder[FcmApnsConfig] = deriveEncoder[FcmApnsConfig]

  implicit val fcmResponseDecoder: Decoder[FcmResponse] = deriveDecoder[FcmResponse]
  implicit val fcmResponseEncoder: Encoder[FcmResponse] = deriveEncoder[FcmResponse]
  implicit val fcmErrorWrapperDecoder: Decoder[FcmErrorWrapper] = deriveDecoder[FcmErrorWrapper]
  implicit val fcmErrorDecoder: Decoder[FcmError] = deriveDecoder[FcmError]
  implicit val fcmErrorDetailDecoder: Decoder[FcmErrorDetail] = deriveDecoder[FcmErrorDetail]
}

/**
  * Possible FCM error codes upon which we act
  */
object FcmErrors {
  val SenderIdMismatch: String = "SENDER_ID_MISMATCH"
  val Unregistered: String = "UNREGISTERED"

  val InvalidTokens: Set[String] = Set(SenderIdMismatch, Unregistered)
}