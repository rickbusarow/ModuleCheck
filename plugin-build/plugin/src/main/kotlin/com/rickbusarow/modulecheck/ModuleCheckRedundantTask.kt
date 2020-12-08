package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.internal.Cli
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import kotlin.system.measureTimeMillis


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
        "${finding.problemName} ${finding.configurationName} dependency: ${finding.logString()}"
      )
      finding.fix()
      ModuleCheckProject.reset()
    }
  }


  protected fun Project.moduleCheckProjects() =
    project.rootProject.allprojects
      .filter { gradleProject -> gradleProject.buildFile.exists() }
      .map { gradleProject -> ModuleCheckProject.from(gradleProject) }

  protected inline fun <T> T.measured(action: T.() -> Unit) {
    val time = measureTimeMillis {
      action()
    }

    cli.printGreen("total parsing time --> $time milliseconds")
  }
}

abstract class ModuleCheckTask : AbstractModuleCheckTask() {

  @TaskAction
  fun execute() = runBlocking {

    val alwaysIgnore = alwaysIgnore.get()
    val ignoreAll = ignoreAll.get()

    measured {

      OverShotProvider(project, alwaysIgnore, ignoreAll).get()
        .finish()

      RedundantProvider(project, alwaysIgnore, ignoreAll).get()
        .finish()

      UnusedProvider(project, alwaysIgnore, ignoreAll).get()
        .finish()
    }

    project.moduleCheckProjects().groupBy { it.findings.getMainDepth() }.toSortedMap()
      .forEach { (depth, modules) ->
        cli.printBlue("""$depth  ${modules.joinToString { it.path }}""")
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

    project.moduleCheckProjects().groupBy { it.findings.getMainDepth() }.toSortedMap()
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

    project.moduleCheckProjects().groupBy { it.findings.getMainDepth() }.toSortedMap()
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

    project.moduleCheckProjects().groupBy { it.findings.getMainDepth() }.toSortedMap()
      .forEach { (depth, modules) ->
        cli.printBlue("""$depth  ${modules.joinToString { it.path }}""")
      }
  }
}
