package com.rustyraven

import sbt._
import Keys._
import com.rustyraven.codebook.ProtocolGenerator

object SbtCodebook extends Plugin {
  val Codebook = config("codebook")

  val codebookGenerate = TaskKey[Seq[File]]("Generate classes from codebook definitions")
  val codebookDecoderPackageName = SettingKey[Option[String]]("Decoder code package name")

  val codebookSettings = inConfig(Codebook)(Seq(
    sourceDirectory <<= (sourceDirectory in Compile) { _ / "codebook"},
    scalaSource <<= (sourceManaged in Compile).apply(_ / "codebook")
//    managedClasspath <<= (configuration, classpathTypes, update) map Classpaths.managedJars
  )) ++ Seq(
    managedSourceDirectories in Compile <+= (scalaSource in Codebook),
    sourceGenerators in Compile <+= (codebookGenerate in Codebook)
  )

  def codebookGeneratorTask:Def.Initialize[Task[Seq[File]]] = Def.task {
    val cachedCompile = FileFunction.cached(streams.value.cacheDirectory / "codebook", FilesInfo.lastModified, FilesInfo.exists) {
      in:Set[File] =>
        ProtocolGenerator.generate(in,(scalaSource in Codebook).value,"scala",(codebookDecoderPackageName in Codebook).value)
    }
    cachedCompile(((sourceDirectory in Codebook).value ** "*.cb").get.toSet).toSeq
  }
}
