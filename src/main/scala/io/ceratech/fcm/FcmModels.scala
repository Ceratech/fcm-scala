package io.ceratech.fcm

import play.api.libs.json.{Json, Reads, Writes}

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
case class FcmResponse(multicast_id: Int, success: Int, failure: Int, canonical_ids: Int, results: Seq[FcmResult])

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
  implicit val fcmNotificationWrites: Writes[FcmNotification] = Json.writes[FcmNotification]

  implicit val fcmResult: Reads[FcmResult] = Json.reads[FcmResult]
  implicit val fcmResponseReads: Reads[FcmResponse] = Json.reads[FcmResponse]
}

/**
  * Possible FCM error codes
  */
object FcmErrors {
  val unregisteredDevice: String = "NotRegistered"
  val invalidRegistration: String = "InvalidRegistration"
}