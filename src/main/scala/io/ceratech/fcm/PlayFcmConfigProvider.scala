package io.ceratech.fcm

import javax.inject.Inject

import play.api.Configuration
import pureconfig.loadConfig

/**
  * Default, Play enabled, FCM config provider
  *
  * @author dries
  */
class PlayFcmConfigProvider @Inject()(configuration: Configuration) extends FcmConfigProvider {

  lazy val config: FcmConfig = loadConfig[FcmConfig](configuration.underlying, "fcm") match {
    case Left(failures) ⇒ throw new IllegalStateException(s"FCM configuration error(s): ${failures.toList.map(_.description).mkString(", ")}")
    case Right(c) ⇒ c
  }

}
