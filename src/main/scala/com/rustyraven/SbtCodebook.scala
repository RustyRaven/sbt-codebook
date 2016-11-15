package com.rustyraven

import sbt._
import Keys._
import com.rustyraven.codebook.ProtocolGenerator
import sbt.plugins.JvmPlugin

object CodebookPlugin extends AutoPlugin {
  val Codebook = config("codebook")

  object autoImport {
    val codebookGenerate = TaskKey[Seq[File]]("generate")
    val withDocument = SettingKey[Boolean]("Generate Document")
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
    codebookGenerate <<= generatorTask
  )) ++ Seq(
    withDocument := false,

    ivyConfigurations += Codebook,
    managedSourceDirectories in Compile <+= (scalaSource in Codebook),
    sourceGenerators in Compile <+= (codebookGenerate in Codebook),
    watchSources <++= sourceDirectory map (path => (path ** "*.cb").get),
    cleanFiles <+= (scalaSource in Codebook)
  )

  def generatorTask:Def.Initialize[Task[Seq[File]]] = Def.task {
    val cachedCompile = FileFunction.cached(streams.value.cacheDirectory / "codebook", FilesInfo.lastModified, FilesInfo.exists) {
      src:Set[File] =>
        ProtocolGenerator.generate(src,
          (scalaSource in Codebook).value,
          "scala",
          None,
          false,
          false,
          withDocument.value)
    }
    cachedCompile(((sourceDirectory in Codebook).value ** "*.cb").get.toSet).toSeq
  }
}
