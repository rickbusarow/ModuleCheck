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
  val alwaysIgnore: SetProperty<String> =
    project.extensions.getByType<ModuleCheckExtension>().alwaysIgnore

  @get:Input
  val ignoreAll: SetProperty<String> =
    project.extensions.getByType<ModuleCheckExtension>().ignoreAll

  @TaskAction
  fun execute() = runBlocking {
    val cli = Cli()

    lateinit var moduleCheckProjects: List<ModuleCheckProject>
    lateinit var findings: List<DependencyFinding>

    val alwaysIgnore = alwaysIgnore.get()
    val ignoreAll = ignoreAll.get()

    val time = measureTimeMillis {

      moduleCheckProjects = project.rootProject.allprojects
        .filter { gradleProject -> gradleProject.buildFile.exists() }
        .map { gradleProject ->
          ModuleCheckProject(gradleProject)
        }

      findings = moduleCheckProjects.sorted()
        .filterNot { moduleCheckProject -> ignoreAll.contains(moduleCheckProject.path) }
        .flatMap { moduleCheckProject ->
          with(moduleCheckProject) {
            listOf(
              overshotDependencies,
              unusedAndroidTest,
              unusedApi,
              unusedCompileOnly,
              unusedImplementation,
              unusedTestImplementation,
              redundantAndroidTest,
              redundantMain,
              redundantTest
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
        .distinctBy { it.position }
    }

    moduleCheckProjects.groupBy { it.mainDepth }.toSortedMap().forEach { (depth, modules) ->
      cli.printBlue("""$depth  ${modules.joinToString { it.path }}""")
    }

    findings.forEach { finding ->
      logger.error("${finding.problemName} ${finding.configurationName} dependency: ${finding.logString()}")
      finding.fix()
    }

    cli.printGreen("total parsing time --> $time milliseconds")

  }
}
