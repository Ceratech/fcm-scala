import Dependencies._

lazy val root = (project in file(".")).
  settings(

    inThisBuild(List(
      organization := "io.ceratech",
      scalaVersion := "2.12.3",
      version      := "0.1.0-SNAPSHOT"
    )),

    name := "fcm-scala",

    bintrayOrganization := Some("ceratech"),
    licenses += ("Apache-2.0", url("http://apache.org/licenses/LICENSE-2.0")),

    libraryDependencies ++= Seq(
      playWs,
      playJson,
      pureconfig,

      scalaTest % Test,
      mockito % Test
    )
  )
