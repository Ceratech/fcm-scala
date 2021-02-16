import Dependencies._

enablePlugins(GitVersioning)

lazy val root = (project in file(".")).
  settings(

    inThisBuild(List(
      organization := "io.ceratech",
      organizationName := "ceratech",
      organizationHomepage := Some(url("https://ceratech.io/")),
      scalaVersion := "2.13.4",
      crossScalaVersions := Seq("2.13.4", "2.12.13")
    )),

    autoScalaLibrary := true,

    name := "fcm-scala",

    git.useGitDescribe := true,
    git.baseVersion := "0.1",

    libraryDependencies ++= sttp,
    libraryDependencies ++= circe,
    libraryDependencies ++= Seq(
      jwtCirce,
      ficus,
      scalaLogging,
      javaxInject,

      scalaTest % Test,
      scalaMock % Test,
      logback % Test
    )
  )

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/Ceratech/fcm-scala"),
    "scm:git@github.com:Ceratech/fcm-scala.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id    = "DriesSchulten",
    name  = "Dries Schulten",
    email = "dries@ceratech.io",
    url   = url("https://ceratech.io/")
  )
)

ThisBuild / description := "FCM (Firebase Cloud Messaging) client for Scala."
ThisBuild / licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / homepage := Some(url("https://github.com/Ceratech/fcm-scala"))

ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true
