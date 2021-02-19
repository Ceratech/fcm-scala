import Dependencies._

enablePlugins(GitVersioning)

lazy val root = (project in file(".")).
  settings(

    inThisBuild(List(
      organization := "io.ceratech",
      organizationName := "ceratech",
      organizationHomepage := Some(url("https://ceratech.io/")),
      homepage := Some(url("https://github.com/scalameta/sbt-scalafmt")),
      description := "FCM (Firebase Cloud Messaging) client for Scala.",
      licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
      scmInfo := Some(
        ScmInfo(
          url("https://github.com/Ceratech/fcm-scala"),
          "scm:git@github.com:Ceratech/fcm-scala.git"
        )
      ),
      developers := List(
        Developer(
          id = "DriesSchulten",
          name = "Dries Schulten",
          email = "dries@ceratech.io",
          url = url("https://ceratech.io/")
        )
      ),
      scalaVersion := "2.13.4",
      crossScalaVersions := Seq("2.13.4", "2.12.13")
    )),

    autoScalaLibrary := true,

    name := "fcm-scala",

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
