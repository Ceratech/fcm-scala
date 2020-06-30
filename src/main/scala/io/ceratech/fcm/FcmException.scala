package io.ceratech.fcm

/**
  * FCM related exception
  *
  * @author dries
  */
case class FcmException(error: String, cause: Throwable = null) extends RuntimeException(error, cause)
