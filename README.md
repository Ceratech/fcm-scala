# Firebase Cloud Messaging wrapper for Play Scala 

[![Build Status](https://travis-ci.org/Ceratech/fcm-scala.svg?branch=master)](https://travis-ci.org/Ceratech/fcm-scala)
[ ![Download](https://api.bintray.com/packages/ceratech/maven/fcm-scala/images/download.svg) ](https://bintray.com/ceratech/maven/fcm-scala/_latestVersion)

This small Scala Library makes it easy to send a notification through the FCM HTTP API. It allows you to:

* Send a message to single/multiple devices
* Handle updated tokens
* Handle deleted/invalid tokens

## Requirements

* Play 2.6.x
* Scala 2.11.x / 2.12.x

## Usage

In your `application.conf` add the following configuration:

```
fcm {
  endpoint = "<Google endpoint e.g.: https://fcm.googleapis.com/fcm/send>"
  key = "<your API key, available in the developer console>"
  dry-run = true # If true FCM will only validate your notifications but not send them!
}
```

Implement the trait `io.ceratech.fcm.TokenRepository` in your codebase to handle updated/deleted tokens from the FCM server. The library expects an instance of the trait to be injectable. E.g. add the binding in code:

```scala
bind(classOf[TokenRepository]).to(classOf[MyTokenRepository])
```

Inject an instance of `io.ceratech.fcm.FcmSender` into your desired class and call `sendNotification(FcmNotification, token)`.

Note: you also need to configure your Play application to use the [WS Client](https://www.playframework.com/documentation/2.6.x/ScalaWS).