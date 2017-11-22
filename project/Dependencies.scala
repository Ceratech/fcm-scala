import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.3"
  lazy val playWs = "com.typesafe.play" %% "play-ahc-ws-standalone" % "1.1.3"
  lazy val playWsJson = "com.typesafe.play" %% "play-ws-standalone-json" % "1.1.3"
  lazy val playJson = "com.typesafe.play" %% "play-json" % "2.6.7"
  lazy val pureconfig = "com.github.pureconfig" %% "pureconfig" % "0.8.0"
  lazy val mockito = "org.mockito" % "mockito-core" % "2.7.22"
  lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"
  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
}
