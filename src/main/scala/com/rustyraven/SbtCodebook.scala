package com.rustyraven

import sbt._
import Keys._
import com.rustyraven.codebook.ProtocolGenerator
import sbt.plugins.JvmPlugin

object CodebookPlugin extends AutoPlugin {
  val Codebook = config("codebook")

  object autoImport {
    val generate = TaskKey[Seq[File]]("generate")
    val codebookDecoderPackageName = SettingKey[Option[String]]("Decoder code package name")
    val debug = TaskKey[Unit]("debug")
  }

  import autoImport._

  private val codebookBuildDependency = SettingKey[ModuleID]("Build dependency")

  /*
    http://stackoverflow.com/questions/25158287/how-do-i-modify-sourcegenerators-in-compile-from-an-autoplugin
    The problem was that the JVM plugin resets the sourceGenerators setting. The solution is just to add:
   */
  override def requires = JvmPlugin

  override lazy val projectSettings = inConfig(Codebook)(Seq(
    sourceDirectory <<= (sourceDirectory in Compile) { _ / "codebook"},
    scalaSource <<= (sourceManaged in Compile).apply(_ / "codebook"),
    generate <<= generatorTask,
    debug <<= debugTask,
    codebookDecoderPackageName := None
//    codebookBuildDependency := "com.rustyraven" %% "codebook" % "1.0-SNAPSHOT"
//    managedClasspath <<= (configuration, classpathTypes, update) map Classpaths.managedJars
  )) ++ Seq(
    ivyConfigurations += Codebook,
    managedSourceDirectories in Compile <+= (scalaSource in Codebook),
    sourceGenerators in Compile <+= (generate in Codebook),
    watchSources <++= sourceDirectory map (path => (path ** "*.cb").get),
    cleanFiles <+= (scalaSource in Codebook)
//    libraryDependencies <+= (codebookBuildDependency in Codebook)
  )

  lazy val debugTask = Def.task {
    val log = streams.value.log
    log.info(s"sourceDirectory:${(sourceDirectory in Codebook).value}")
    log.info(s"scalaSource:${(scalaSource in Codebook).value}")
    log.info(s"watchSources:${(sourceDirectory map (path => (path ** "*.cb").get)).value}")
    log.info(s"generate:${(generate in Codebook).value}")
  }

  def generatorTask:Def.Initialize[Task[Seq[File]]] = Def.task {
    val cachedCompile = FileFunction.cached(streams.value.cacheDirectory / "codebook", FilesInfo.lastModified, FilesInfo.exists) {
      src:Set[File] =>
        ProtocolGenerator.generate(src,(scalaSource in Codebook).value,"scala",(codebookDecoderPackageName in Codebook).value)
    }
    cachedCompile(((sourceDirectory in Codebook).value ** "*.cb").get.toSet).toSeq
  }
}
