package io.ceratech.fcm

/**
  * Provides the FCM config to use when sending notifications
  *
  * @author dries
  */
trait FcmConfigProvider {

  /**
    * @return the FCM condig to use
    */
  val config: FcmConfig
}
