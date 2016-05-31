package com.rustyraven

import sbt._
import Keys._
import com.rustyraven.codebook.ProtocolGenerator

object SbtCodebook extends Plugin {
  val Codebook = config("codebook")

//  val codebookTargetLanguage = SettingKey[String]("Code generation target language")
  val codebookGenerate = TaskKey[Seq[File]]("Generate classes from codebook definitions")

  val codebookSettings = inConfig(Codebook)(Seq(
    sourceDirectory <<= (sourceDirectory in Compile) { _ / "codebook"},
    scalaSource <<= (sourceManaged in Compile).apply(_ / "codebook")
//    managedClasspath <<= (configuration, classpathTypes, update) map Classpaths.managedJars
  )) ++ Seq(
    managedSourceDirectories in Compile <+= (scalaSource in Codebook),
    sourceGenerators in Compile <+= (codebookGenerate in Codebook)
  )

  def codebookGenerate(srcFiles:Set[File],targetBaseDir:File,log:Logger):Set[File] = {
    srcFiles.map(src=>ProtocolGenerator.generate(src,targetBaseDir,"scala")).flatten
  }

  def codebookGeneratorTask:Def.Initialize[Task[Seq[File]]] = Def.task {
    val cachedCompile = FileFunction.cached(streams.value.cacheDirectory / "codebook", FilesInfo.lastModified, FilesInfo.exists) {
      in:Set[File] =>
        codebookGenerate(
          in,
          (scalaSource in Codebook).value,
          streams.value.log)
    }
    cachedCompile(((sourceDirectory in Codebook).value ** "*.cb").get.toSet).toSeq
  }
}
