lazy val root = project
  .in(file("."))
  .settings(
    name         := "DataV",
    organization := "visionofsid",
    scalaVersion := "2.12.8",
    version      := "0.1.0-SNAPSHOT",
    libraryDependencies += "dev.zio" %% "zio" % "1.0.4-2",
    libraryDependencies += "dev.zio" %% "zio-streams" % "1.0.4-2"
  )









