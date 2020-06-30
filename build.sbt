import Dependencies._

enablePlugins(GitVersioning)

lazy val root = (project in file(".")).
  settings(

    inThisBuild(List(
      organization := "io.ceratech",
      scalaVersion := "2.13.3",
      crossScalaVersions := Seq("2.13.3", "2.12.11")
    )),

    autoScalaLibrary := true,

    name := "fcm-scala",

    bintrayOrganization := Some("ceratech"),
    licenses += ("Apache-2.0", url("http://apache.org/licenses/LICENSE-2.0")),

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
