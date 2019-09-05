lazy val codebook = (project in file("."))
    .settings(
    name := "sbt-codebook",
    organization := "com.rusty-raven",
    version := "1.2-SNAPSHOT",
    sbtPlugin := true,

    scalaVersion := "2.12.4",
    sbtVersion := "1.0.4",
//    crossSbtVersions := Vector("1.0.4", "0.13.16"),
    scalaCompilerBridgeSource := {
      val sv = appConfiguration.value.provider.id.version
      ("org.scala-sbt" % "compiler-interface" % sv % "component").sources
    },

    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    libraryDependencies ++= Seq(
      "com.rusty-raven" %% "codebook" % "1.2-SNAPSHOT"),

    publishTo := Some(
      if (isSnapshot.value)
        Opts.resolver.sonatypeSnapshots
      else
        Opts.resolver.sonatypeStaging
    )
  )
