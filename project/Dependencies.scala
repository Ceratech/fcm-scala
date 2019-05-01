import sbt._

object Dependencies {
  private val circeVersion = "0.10.1"
  private val sttpVersion = "1.4.2"
  private val retryVersion = "0.3.2"

  lazy val sttp = Seq(
    "com.softwaremill.sttp" %% "core" % sttpVersion,
    "com.softwaremill.sttp" %% "circe" % sttpVersion,
    "com.softwaremill.sttp" %% "async-http-client-backend-future" % sttpVersion,
    "com.softwaremill.retry" %% "retry" % retryVersion
  )

  lazy val circe = Seq(
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion
  )

  lazy val jwtCirce = "com.pauldijou" %% "jwt-circe" % "0.19.0"
  lazy val javaxInject = "javax.inject" % "javax.inject" % "1"
  lazy val ficus = "com.iheart" %% "ficus" % "1.4.3"
  lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5"
  lazy val scalaMock = "org.scalamock" %% "scalamock" % "4.1.0"
  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
}
