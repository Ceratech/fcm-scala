import Dependencies._

enablePlugins(GitVersioning)

lazy val root = (project in file(".")).
  settings(

    inThisBuild(List(
      organization := "io.ceratech",
      scalaVersion := "2.12.3"
    )),

    name := "fcm-scala",

    bintrayOrganization := Some("ceratech"),
    licenses += ("Apache-2.0", url("http://apache.org/licenses/LICENSE-2.0")),

    git.useGitDescribe := true,
    git.baseVersion := "0.1",

    libraryDependencies ++= Seq(
      playWs,
      playJson,
      pureconfig,

      scalaTest % Test,
      mockito % Test
    )
  )
