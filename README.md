# Firebase Cloud Messaging wrapper for Scala 

[![CI](https://github.com/Ceratech/fcm-scala/actions/workflows/build.yml/badge.svg)](https://github.com/Ceratech/fcm-scala/actions/workflows/build.yml)
[![Download](https://api.bintray.com/packages/ceratech/maven/fcm-scala/images/download.svg) ](https://bintray.com/ceratech/maven/fcm-scala/_latestVersion)
[![codecov](https://codecov.io/gh/Ceratech/fcm-scala/branch/master/graph/badge.svg?token=cxPA8zCaLN)](https://codecov.io/gh/Ceratech/fcm-scala)

This small Scala Library makes it easy to send a notification through the FCM HTTP API. It allows you to:

* Send a message to single/multiple devices
* Handle updated tokens
* Handle deleted/invalid tokens

This library makes use of the [STTP HTTP Client](https://sttp.readthedocs.io/en/latest/) and the [Circe JSON library](circe.github.io/circe/).

## Requirements

* Scala 2.13.x/2.12.x

## Usage

### Configuration

Either construct an instance of `DefaultFcmSender` and supply the necessary parameters or use your favorite DI framework.

#### DI configuration (using Google Guice)

Bind the `DefaultFcmSender` to use it in your application:

```scala
bind(classOf[FcmSender]).to(classOf[DefaultFcmSender])
``` 

Bind the `DefaultFirebaseAuthenticator`:

```scala
bind(classOf[FirebaseAuthenticator]).to(classOf[DefaultFirebaseAuthenticator])
```

#### FCM configuration

The `FcmSender` needs configuration, easiest is to use a [Typesafe Config](https://github.com/lightbend/config) file to configure FCM. In your `application.conf` add the following configuration:

```
fcm {
  endpoint = "<Google endpoint e.g.: https://fcm.googleapis.com/v1/{parent=projects/*}/messages:send"
  key-file = "Path to the JSON key file"
  validate-only = true # If true FCM will only validate your notifications but not send them!
  token-endpoint = "<Google token endpoint, optional, defaults to: https://www.googleapis.com/oauth2/v4/token>"
}
```

Bind the `DefaultFcmConfigProvider` dependency to read the configuration from the `application.conf`, in case of Google Guice:

```scala
bind(classOf[FcmConfigProvider]).to(classOf[DefaultFcmConfigProvider])
```

You can also provide your own implementation of the `FcmConfigProvider` trait to get needed configuration.

By default the `FcmConfigProvider` defaults to an async based STTP client; this can be overriden in your own `FcmConfigProvider` implementation. 

### Token repository

Implement the trait `io.ceratech.fcm.TokenRepository` in your codebase to handle deleted tokens from the FCM server. The library expects an instance of the trait to be injectable. E.g. add the binding in code:

```scala
bind(classOf[TokenRepository]).to(classOf[MyTokenRepository])
```

### Sending notifications

Inject an instance of `io.ceratech.fcm.FcmSender` into your desired class and call `sendMessage(FcmNotification, token)`. The `FcmMessage` case class contains the fields you can supply to FCM.
