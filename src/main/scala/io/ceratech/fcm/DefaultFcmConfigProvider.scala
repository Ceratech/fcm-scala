package io.ceratech.fcm

import javax.inject.Inject

import com.typesafe.config.Config
import pureconfig.loadConfig

/**
  * Default, TypeSafe config enabled, FCM config provider
  *
  * @author dries
  */
class DefaultFcmConfigProvider @Inject()(configuration: Config) extends FcmConfigProvider {

  lazy val config: FcmConfig = loadConfig[FcmConfig](configuration, "fcm") match {
    case Left(failures) ⇒ throw new IllegalStateException(s"FCM configuration error(s): ${failures.toList.map(_.description).mkString(", ")}")
    case Right(c) ⇒ c
  }

}
