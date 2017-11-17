package io.ceratech.fcm

/**
  * FCM related exception
  *
  * @author dries
  */
case class FcmException(error: String) extends RuntimeException(error)
