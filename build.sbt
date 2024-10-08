import sbt.Keys.{publishMavenStyle, _}

resolvers in Global += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

scalaVersion := "2.12.19"

val codebookVersion = "1.23.0-SNAPSHOT"

val codebookLibrary = Seq("com.rusty-raven" %% "codebook" % codebookVersion changing())

lazy val codebook = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-codebook",
    organization := "com.rusty-raven",
    scalaVersion := "2.12.19",
    version := codebookVersion,
    sbtPlugin := true,

//    scalaVersion := "2.12.9",
//    sbtVersion := "1.0.4",
//    crossSbtVersions := Vector("1.0.4", "0.13.16"),
//    scalaCompilerBridgeSource := {
//      val sv = appConfiguration.value.provider.id.version
//      ("org.scala-sbt" % "compiler-interface" % sv % "component").sources
//    },
//    pluginCrossBuild / sbtVersion := {
//        scalaBinaryVersion.value match {
//          case "2.12" => "1.2.8" // set minimum sbt version
//        }
//      },

//    resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    libraryDependencies ++= codebookLibrary,

//    publishTo := Some(
//      if (isSnapshot.value)
//        Opts.resolver.sonatypeSnapshots
//      else
//        Opts.resolver.sonatypeStaging
//    )
    publishMavenStyle := true,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },

    Test / publishArtifact := false,
    pomIncludeRepository := { _ => false },
    sonatypeProfileName := "com.rusty-raven",
    pomExtra :=
      <url>https://github.com/RustyRaven/sbt-codebook</url>
        <licenses>
          <license>
            <name>MIT</name>
            <url>https://opensource.org/licenses/MIT</url>
          </license>
        </licenses>
        <scm>
          <url>https://github.com/RustyRaven/sbt-codebook</url>
          <connection>https://github.com/RustyRaven/sbt-codebook.git</connection>
        </scm>
        <developers>
          <developer>
            <id>OsamuTakahashi</id>
            <name>Osamu Takahashi</name>
            <url>https://github.com/OsamuTakahashi/</url>
          </developer>
        </developers>
  )
