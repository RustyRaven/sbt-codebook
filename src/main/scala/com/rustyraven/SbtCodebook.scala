package com.rustyraven

import sbt.{Def, *}
import Keys.*
import com.rustyraven.codebook.{GeneratorOptions, ProtocolGenerator}
import sbt.plugins.JvmPlugin
import sbt.complete.DefaultParsers.*
import sbt.internal.io.Source

object CodebookPlugin extends AutoPlugin {
  val Codebook = config("codebook")

  object autoImport {
    val codebookGenerate = TaskKey[Seq[File]]("generate")
    val withDocument = SettingKey[Boolean]("generate document")
    val withTestClient = SettingKey[Boolean]("generate with TestClient code")
    val documentSourceDir = SettingKey[String]("document source directory")
    val clientLanguage = SettingKey[String]("client language")
    val skeleton = InputKey[Unit]("skeleton")
    val document = TaskKey[Unit]("document")
    val clientCode = InputKey[Unit]("clientCode")
    val migration = TaskKey[Unit]("migration")
    val info = TaskKey[Unit]("info")
  }

  import autoImport._

  private val codebookBuildDependency = SettingKey[ModuleID]("build dependency")

  /*
    http://stackoverflow.com/questions/25158287/how-do-i-modify-sourcegenerators-in-compile-from-an-autoplugin
    The problem was that the JVM plugin resets the sourceGenerators setting. The solution is just to add:
   */
  override def requires = JvmPlugin

  override lazy val projectSettings = inConfig(Codebook)(Seq(
    sourceDirectory := (Compile / sourceDirectory).value / "codebook",
    scalaSource := (Compile / sourceManaged).value / "codebook",
    codebookGenerate := generatorTask.value,
    skeleton := skeletonTask.evaluated,
    document := documentTask.value,
    clientCode := clientCodeTask.evaluated,
    migration := migrationTask.value,
    info := infoTask.value
  )) ++ Seq(
    withDocument := false,
    withTestClient := false,
    documentSourceDir := "docsrc",
    clientLanguage := "csharp",

    ivyConfigurations += Codebook,
    Compile / managedSourceDirectories += (Codebook / scalaSource).value,
    Compile / sourceGenerators += (Codebook / codebookGenerate).taskValue,
    watchSources += new Source(sourceDirectory.value, "*.cb", HiddenFileFilter),
    cleanFiles += (Codebook / scalaSource).value
  )

  private val buildMapping: Def.Initialize[Task[Seq[Def.Initialize[(ProjectRef, Seq[Seq[File]])]]]] = {
    Def.taskDyn {
      val refs = loadedBuild.value.allProjectRefs

      val tt = refs.map(_._1).map {
        ref =>
          sourceDirectories.all(ScopeFilter(inProjects(ref)))
            .zipWith(Def.setting(ref)) { case (a, b) => b -> a }
      }

      Def.task {
        tt
      }
    }
  }
  
  def generatorTask:Def.Initialize[Task[Seq[File]]] = Def.task {
    val cachedCompile = FileFunction.cached(streams.value.cacheDirectory / "codebook", FilesInfo.lastModified, FilesInfo.exists) {
      src:Set[File] =>
        ProtocolGenerator.generate(src,
          Set.empty,
          (Codebook / scalaSource).value,
          "scala",
          None,
          withTestClient.value,
          false,
          withDocument.value,false)
    }
    cachedCompile(((Codebook / sourceDirectory).value ** "*.cb").get.toSet).toSeq
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
    ProtocolGenerator.generate(
      _sources,
      Set.empty[File],
      new File(baseDirectory.value,"skeleton"),
      "scala",None,GeneratorOptions(false,false,category.forall(_ => false),true,category))
  }

  def documentTask:Def.Initialize[Task[Unit]] = Def.task {
    val _sources = (sourceDirectory map (path => (path ** "*.cb").get)).value.toSet
    ProtocolGenerator.generate(
      _sources,
      Set.empty[File],
      new File(baseDirectory.value,documentSourceDir.value),
      "sphinx",
      None,
      GeneratorOptions())
  }

  def clientCodeTask:Def.Initialize[InputTask[Unit]] = Def.inputTask {
    val args = spaceDelimited("<language>").parsed
    val lang = args.length match {
      case 0 =>
        "csharp"
      case _ =>
        args.head
    }
    val _sources = (sourceDirectory map (path => (path ** "*.cb").get)).value.toSet
    ProtocolGenerator.generate(
      _sources,
      Set.empty[File],
      new File(baseDirectory.value,s"clientCode/$lang"),
      lang,
      Some(new File(baseDirectory.value,documentSourceDir.value)),
      GeneratorOptions(withClientCode = true,withDocument = true))
  }

  def migrationTask:Def.Initialize[Task[Unit]] = Def.task {
    val _sources = (sourceDirectory map (path => (path ** "*.cb").get)).value.toSet
    ProtocolGenerator.generate(
      _sources,
      Set.empty[File],
      new File(baseDirectory.value,"src/main/migrations"),
      "liquibase",
      None,
      GeneratorOptions())
  }

  private def flattenTasks[A](tasks: Seq[Def.Initialize[Task[A]]]): Def.Initialize[Task[List[A]]] =
    tasks.toList match {
      case Nil => Def.task { Nil }
      case x :: xs => Def.taskDyn { flattenTasks(xs) map (x.value :: _) }
    }
  
  private def depProjects: Def.Initialize[Task[List[Set[File]]]] = Def.taskDyn {
    val pdeps = projectDependencies.value
    val projects = loadedBuild.value.allProjectRefs.map(_._1)
    val fss = projects.map {
      projectRef =>
        Def.taskDyn {
          val pid = (projectRef / projectID).value
          val fs = if (pdeps.exists(mid => mid.name == pid.name)) {
            val srs = (projectRef / sourceDirectory).map(path => (path ** "*.cb").get).value.toSet
            srs
          } else {
            Set.empty[File]
          }
          Def.task {
            fs
          }
        }
    }
    flattenTasks(fss)
  }

  private def referenceFiles: Def.Initialize[Task[Set[File]]] = Def.task {
    depProjects.value.reduce(_ ++ _)
  }

  def infoTask:Def.Initialize[Task[Unit]] = Def.task {
    val r = referenceFiles.value
    println(r)
  }
}
