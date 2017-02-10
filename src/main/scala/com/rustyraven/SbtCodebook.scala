package com.rustyraven

import sbt._
import Keys._
import com.rustyraven.codebook.{GeneratorOptions, ProtocolGenerator}
import sbt.plugins.JvmPlugin
import sbt.complete.DefaultParsers._

object CodebookPlugin extends AutoPlugin {
  val Codebook = config("codebook")

  object autoImport {
    val codebookGenerate = TaskKey[Seq[File]]("generate")
    val withDocument = SettingKey[Boolean]("Generate Document")
    val documentSourceDir = SettingKey[String]("Document source directory")
    val skeleton = InputKey[Unit]("skeleton")
    val document = TaskKey[Unit]("document")
    val clientCode = InputKey[Unit]("clientCode")
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
    codebookGenerate <<= generatorTask,
    skeleton <<= skeletonTask,
    document <<= documentTask,
    clientCode <<= clientCodeTask
  )) ++ Seq(
    withDocument := false,
    documentSourceDir := "docsrc",

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

  def skeletonTask:Def.Initialize[InputTask[Unit]] = Def.inputTask {
    val args = spaceDelimited("<arg>").parsed
    val category = args.length match {
      case 0 =>
        None
      case _ =>
        Some(args.head)
    }
    val _sources = (sourceDirectory map (path => (path ** "*.cb").get)).value.toSet
    ProtocolGenerator.generate(_sources,new File(baseDirectory.value,"skeleton"),"scala",None,GeneratorOptions(false,false,category.forall(_ => false),true,category))
  }

  def documentTask:Def.Initialize[Task[Unit]] = Def.task {
    val _sources = (sourceDirectory map (path => (path ** "*.cb").get)).value.toSet
    ProtocolGenerator.generate(_sources,new File(baseDirectory.value,documentSourceDir.value),"sphinx",None,GeneratorOptions())
  }

  def clientCodeTask:Def.Initialize[InputTask[Unit]] = Def.inputTask {
    val args = spaceDelimited("<arg>").parsed
    val lang = args.length match {
      case 0 =>
        "csharp"
      case _ =>
        args.head
    }
    val _sources = (sourceDirectory map (path => (path ** "*.cb").get)).value.toSet
    ProtocolGenerator.generate(_sources,new File(baseDirectory.value,s"clientCode/$lang"),lang,Some(new File(baseDirectory.value,documentSourceDir.value)),GeneratorOptions(withClientCode = true,withDocument = true))
  }
}
