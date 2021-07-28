ThisBuild / versionScheme := Some("semver-spec")

lazy val importer = crossProject(JSPlatform, JVMPlatform, NativePlatform).in(file(".")).
  settings(
    name := "importer",
    version := "0.1.4",
    scalaVersion := "2.13.6",
    scalacOptions ++=
      Seq(
        "-deprecation", "-feature", "-unchecked",
        "-language:postfixOps", "-language:implicitConversions", "-language:existentials", "-language:dynamics",
        "-Xasync"
      ),
    organization := "xyz.hyperreal",
    githubOwner := "edadma",
    githubRepository := "importer",
    mainClass := Some("xyz.hyperreal.importer.Main"),
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.9" % "test",
    libraryDependencies ++=
      Seq(
        "xyz.hyperreal" %%% "char-reader" % "0.1.0",
        "xyz.hyperreal" %%% "datetime" % "0.1.0",
      ),
    publishMavenStyle := true,
    Test / publishArtifact := false,
    licenses += "ISC" -> url("https://opensource.org/licenses/ISC")
  ).
  jvmSettings(
    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % "1.0.0" % "provided",
  ).
  nativeSettings(
    nativeLinkStubs := true
  ).
  jsSettings(
    jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv(),
    Test / mainClass := Some("xyz.hyperreal.importer.Main"),
    Test / scalaJSUseMainModuleInitializer := true,
    Test / scalaJSUseTestModuleInitializer := false,
//    Test / scalaJSUseMainModuleInitializer := false,
//    Test / scalaJSUseTestModuleInitializer := true,
    scalaJSUseMainModuleInitializer := true,
  )
