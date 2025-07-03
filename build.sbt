ThisBuild / licenses += "ISC"      -> url("https://opensource.org/licenses/ISC")
ThisBuild / versionScheme          := Some("semver-spec")
ThisBuild / evictionErrorLevel     := Level.Warn
ThisBuild / scalaVersion           := "3.7.1"
ThisBuild / organization           := "io.github.edadma"
ThisBuild / organizationName       := "edadma"
ThisBuild / organizationHomepage   := Some(url("https://github.com/edadma"))
ThisBuild / version                := "0.0.3"
ThisBuild / sonatypeCredentialHost := "central.sonatype.com"

ThisBuild / publishConfiguration := publishConfiguration.value.withOverwrite(true).withChecksums(Vector.empty)
ThisBuild / resolvers += Resolver.mavenLocal
ThisBuild / resolvers += Resolver.sonatypeCentralSnapshots
ThisBuild / resolvers += Resolver.sonatypeCentralRepo("releases")

ThisBuild / sonatypeProfileName := "io.github.edadma"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/edadma/importer"),
    "scm:git@github.com:edadma/importer.git",
  ),
)
ThisBuild / developers := List(
  Developer(
    id = "edadma",
    name = "Edward A. Maxedon, Sr.",
    email = "edadma@gmail.com",
    url = url("https://github.com/edadma"),
  ),
)

ThisBuild / homepage := Some(url("https://github.com/edadma/importer"))

ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo            := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}
ThisBuild / publishMavenStyle := true

lazy val importer = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .in(file("."))
  .settings(
    name := "importer",
    scalacOptions ++=
      Seq(
        "-deprecation",
        "-feature",
        "-unchecked",
        "-language:postfixOps",
        "-language:implicitConversions",
        "-language:existentials",
        "-language:dynamics",
      ),
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.19" % "test",
    libraryDependencies ++=
      Seq(
        "io.github.edadma" %%% "char-reader" % "0.1.20",
      ),
    publishMavenStyle      := true,
    Test / publishArtifact := false,
    licenses += "ISC"      -> url("https://opensource.org/licenses/ISC"),
  )
  .jvmSettings(
    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "1.1.0" % "provided",
    libraryDependencies += "com.lihaoyi" %%% "pprint"        % "0.9.0" % "test",
  )
  .nativeSettings(
    libraryDependencies += "org.scala-js"       %% "scalajs-stubs"   % "1.1.0" % "provided",
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.6.0",
  )
  .jsSettings(
    jsEnv            := new org.scalajs.jsenv.nodejs.NodeJSEnv(),
    Test / mainClass := Some(s"${organization.value}.${name.value}.Main"),
//    Test / scalaJSUseMainModuleInitializer := true,
//    Test / scalaJSUseTestModuleInitializer := false,
    Test / scalaJSUseMainModuleInitializer      := false,
    Test / scalaJSUseTestModuleInitializer      := true,
    scalaJSUseMainModuleInitializer             := true,
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.6.0",
  )

lazy val root = project
  .in(file("."))
  .aggregate(importer.js, importer.jvm, importer.native)
  .settings(
    name                := "importer",
    publish / skip      := true,
    publishLocal / skip := true,
  )
