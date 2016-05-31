import sbt._
import sbt.Keys._

object SbtCodebookBuild extends Build {

  lazy val codebook = Project(
    id = "sbt-codebook",
    base = file("."),

    settings = Seq(
//      scalaVersion := "2.11.7",
//      scalacOptions ++= Seq("-deprecation","-feature","-language:implicitConversions"),
      name := "sbt-codebook",
      organization := "com.rustyraven",
      version := "1.0-SNAPSHOT",
      sbtPlugin := true,

      resolvers ++= Seq("RustyRaven Repository" at "http://rustyraven.github.io"),
      libraryDependencies ++= Seq(
        "com.rustyraven" %% "codebook" % "1.0-SNAPSHOT"),

      publishTo := Some(Resolver.file("sbt-codebook",file("../RustyRaven.github.io"))(Patterns(true, Resolver.mavenStyleBasePattern))))
    )
}
