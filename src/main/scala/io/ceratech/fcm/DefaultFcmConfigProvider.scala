package io.ceratech.fcm

import com.typesafe.config.Config
import javax.inject.Inject
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

/**
  * Default, TypeSafe config enabled, FCM config provider
  *
  * @author dries
  */
class DefaultFcmConfigProvider @Inject()(configuration: Config) extends FcmConfigProvider {

  import net.ceedubs.ficus.readers.namemappers.implicits.hyphenCase

  lazy val config: FcmConfig = configuration.as[FcmConfig]("fcm")
}
