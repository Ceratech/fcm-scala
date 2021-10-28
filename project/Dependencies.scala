import sbt._

object Dependencies {
  private val circeVersion = "0.14.1"
  private val sttpVersion = "3.3.16"
  private val retryVersion = "0.3.3"

  lazy val sttp = Seq(
    "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
    "com.softwaremill.sttp.client3" %% "circe" % sttpVersion,
    "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % sttpVersion,
    "com.softwaremill.retry" %% "retry" % retryVersion
  )

  lazy val circe = Seq(
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion
  )

  lazy val jwtCirce = "com.pauldijou" %% "jwt-circe" % "5.0.0"
  lazy val javaxInject = "javax.inject" % "javax.inject" % "1"
  lazy val ficus = "com.iheart" %% "ficus" % "1.5.1"
  lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.9"
  lazy val scalaMock = "org.scalamock" %% "scalamock" % "5.1.0"
  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.2.6"
}
