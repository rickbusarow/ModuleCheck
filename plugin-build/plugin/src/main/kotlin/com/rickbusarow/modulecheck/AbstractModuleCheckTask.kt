package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.internal.Cli
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType


abstract class AbstractModuleCheckTask : DefaultTask() {

  init {
    description = "verification"
  }

  @get:Input
  val alwaysIgnore: SetProperty<String> =
    project.extensions.getByType<ModuleCheckExtension>().alwaysIgnore

  @get:Input
  val ignoreAll: SetProperty<String> =
    project.extensions.getByType<ModuleCheckExtension>().ignoreAll

  protected val cli = Cli()

  protected fun List<DependencyFinding>.finish() {

    forEach { finding ->

      project.logger.error(
        "${finding.problemName} ${finding.config.name} dependency: ${finding.logString()}"
      )
      finding.fix()
      MCP.reset()
    }
  }

  protected fun Project.moduleCheckProjects() =
    project.rootProject.allprojects
      .filter { gradleProject -> gradleProject.buildFile.exists() }
      .map { gradleProject -> MCP.from(gradleProject) }

  protected inline fun <T, R> T.measured(action: T.() -> R): R {

    var r: R? = null

    val time = measureTimeMillis {
      r = action()
    }

    project.moduleCheckProjects()
      .forEach { mcp ->
        println(
          """project --> $mcp
          |
          |jvm files --> ${mcp.mainFiles.joinToString("\n")}
        """.trimMargin()
        )
      }

    cli.printGreen("total parsing time --> $time milliseconds")

    return r!!
  }
}

abstract class ModuleCheckTask : AbstractModuleCheckTask() {

  @TaskAction
  fun execute() = runBlocking {

    val alwaysIgnore = alwaysIgnore.get()
    val ignoreAll = ignoreAll.get()

    measured {

      val all = OverShotProvider(project, alwaysIgnore, ignoreAll).get() +
        RedundantProvider(project, alwaysIgnore, ignoreAll).get() +
        UnusedProvider(project, alwaysIgnore, ignoreAll).get()

      all.distinctBy { it.dependentProject to CPP(it.config, it.dependencyProject) }
        .finish()
    }

    project.moduleCheckProjects().groupBy { it.getMainDepth() }.toSortedMap()
      .forEach { (depth, modules) ->
        cli.printBlue("""$depth  ${modules.joinToString { it.path }}""")
      }
  }
}

abstract class ModuleCheckUsedTask : AbstractModuleCheckTask() {

  @TaskAction
  fun execute() = runBlocking {

    val pairs = measured {

      project
        .moduleCheckProjects()
        .map { mcp ->
          mcp to mcp.resolvedMainDependencies
        }
    }

    pairs
      .sortedBy { it.first }
      .forEach { (mcp, lst) ->
        cli.printYellow("${mcp.path.padEnd(50)} -- ${lst.joinToString { it.project.path }}")
      }
  }
}

abstract class ModuleCheckOverShotTask : AbstractModuleCheckTask() {

  @TaskAction
  fun execute() = runBlocking {

    val alwaysIgnore = alwaysIgnore.get()
    val ignoreAll = ignoreAll.get()

    measured {

      OverShotProvider(project, alwaysIgnore, ignoreAll).get()
        .finish()
    }

    project.moduleCheckProjects().groupBy { it.getMainDepth() }.toSortedMap()
      .forEach { (depth, modules) ->
        cli.printBlue("""$depth  ${modules.joinToString { it.path }}""")
      }
  }
}

abstract class ModuleCheckRedundantTask : AbstractModuleCheckTask() {

  @TaskAction
  fun execute() = runBlocking {

    val alwaysIgnore = alwaysIgnore.get()
    val ignoreAll = ignoreAll.get()

    measured {

      RedundantProvider(project, alwaysIgnore, ignoreAll).get()
        .finish()
    }

    project.moduleCheckProjects().groupBy { it.getMainDepth() }.toSortedMap()
      .forEach { (depth, modules) ->
        cli.printBlue("""$depth  ${modules.joinToString { it.path }}""")
      }
  }
}

abstract class ModuleCheckUnusedTask : AbstractModuleCheckTask() {

  @TaskAction
  fun execute() = runBlocking {

    val alwaysIgnore = alwaysIgnore.get()
    val ignoreAll = ignoreAll.get()

    measured {

      UnusedProvider(project, alwaysIgnore, ignoreAll).get()
        .finish()
    }

    project.moduleCheckProjects().groupBy { it.getMainDepth() }.toSortedMap()
      .forEach { (depth, modules) ->
        cli.printBlue("""$depth  ${modules.joinToString { it.path }}""")
      }
  }
}
