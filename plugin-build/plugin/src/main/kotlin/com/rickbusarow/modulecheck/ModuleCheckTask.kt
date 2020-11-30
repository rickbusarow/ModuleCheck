package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.internal.Cli
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import kotlin.system.measureTimeMillis

abstract class ModuleCheckTask : DefaultTask() {

  init {
    description = "verification"
  }

  @get:Input
  val alwaysIgnore: SetProperty<String> = project.extensions.getByType<ModuleCheckExtension>().alwaysIgnore

  @get:Input
  val ignoreAll: SetProperty<String> = project.extensions.getByType<ModuleCheckExtension>().ignoreAll

  @TaskAction
  fun execute() = runBlocking {
    val cli = Cli()

    lateinit var moduleCheckProjects: List<ModuleCheckProject>
    lateinit var unused: List<UnusedDependency>

    val alwaysIgnore = alwaysIgnore.get()
    val ignoreAll = ignoreAll.get()

    val time = measureTimeMillis {

      moduleCheckProjects = project.rootProject.allprojects
        .filter { gradleProject -> gradleProject.buildFile.exists() }
        .map { gradleProject ->
          ModuleCheckProject(gradleProject)
        }

      unused = moduleCheckProjects.sorted()
        .filterNot { moduleCheckProject -> ignoreAll.contains(moduleCheckProject.path) }
        .flatMap { moduleCheckProject ->
          with(moduleCheckProject) {
            listOf(
              unusedAndroidTest,
              unusedApi,
              unusedCompileOnly,
              unusedImplementation,
              unusedTestImplementation
            ).flatMap { dependencies ->
              dependencies.mapNotNull { dependency ->
                if (alwaysIgnore.contains(dependency.dependencyPath)) {
                  null
                } else {
                  dependency
                }
              }
            }
          }
        }
    }

    moduleCheckProjects.groupBy { it.mainDepth }.toSortedMap().forEach { (depth, modules) ->
      cli.printBlue("""$depth  ${modules.joinToString { it.path }}""")
    }

    unused.forEach { dependency ->
      logger.error("unused ${dependency.configurationName} dependency: ${dependency.logString()}")
    }

    cli.printGreen("total parsing time --> $time milliseconds")

  }
}
