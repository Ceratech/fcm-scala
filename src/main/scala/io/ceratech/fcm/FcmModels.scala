package io.ceratech.fcm

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

/**
  * Message that gets send to FCM
  *
  * @author dries
  */
case class FcmMessage(registrationIds: Seq[String], dryRun: Boolean, notification: FcmNotification)

/**
  * Notification
  *
  * @author dries
  */
case class FcmNotification(body: Option[String], title: Option[String] = None, badge: Option[String] = None)

/**
  * FCM response object
  *
  * @author dries
  */
case class FcmResponse(multicast_id: Long, success: Int, failure: Int, canonical_ids: Int, results: Seq[FcmResult])

/**
  * An individual message result
  *
  * @author dries
  */
case class FcmResult(message_id: Option[String], registration_id: Option[String], error: Option[String])

/**
  * JSON formats
  */
object FcmJsonFormats {
  implicit val fcmMessageEncoder: Encoder[FcmMessage] = (m: FcmMessage) => Json.obj(
    ("registration_ids", Json.arr(m.registrationIds.map(Json.fromString): _*)),
    ("dry_run", Json.fromBoolean(m.dryRun)),
    ("notification", m.notification.asJson)
  )
  implicit val fcmNotificationEncoder: Encoder[FcmNotification] = deriveEncoder[FcmNotification]

  implicit val fcmResultDecoder: Decoder[FcmResult] = deriveDecoder[FcmResult]
  implicit val fcmResultEncoder: Encoder[FcmResult] = deriveEncoder[FcmResult]
  implicit val fcmResponseDecoder: Decoder[FcmResponse] = deriveDecoder[FcmResponse]
  implicit val fcmResponseEncoder: Encoder[FcmResponse] = deriveEncoder[FcmResponse]
}

/**
  * Possible FCM error codes
  */
object FcmErrors {
  val unregisteredDevice: String = "NotRegistered"
  val invalidRegistration: String = "InvalidRegistration"
}