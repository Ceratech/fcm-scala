import sbt._

object Dependencies {
  private val playVersion = "2.6.7"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.3"
  lazy val playWs = "com.typesafe.play" %% "play-ws" % playVersion
  lazy val playJson = "com.typesafe.play" %% "play-json" % playVersion
  lazy val pureconfig = "com.github.pureconfig" %% "pureconfig" % "0.8.0"
  lazy val mockito = "org.mockito" % "mockito-core" % "2.7.22"
}
