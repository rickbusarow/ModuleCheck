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

  fun Project.moduleCheckProjects() =
    project.rootProject.allprojects
      .filter { gradleProject -> gradleProject.buildFile.exists() }
      .map { gradleProject -> ModuleCheckProject.from(gradleProject) }

  @TaskAction
  fun execute() = runBlocking {
    val cli = Cli()

    val alwaysIgnore = alwaysIgnore.get()
    val ignoreAll = ignoreAll.get()

    val time = measureTimeMillis {

      val projects = project.moduleCheckProjects()

      projects
        .sorted()
        .filterNot { moduleCheckProject -> moduleCheckProject.path in ignoreAll }
        .flatMap { moduleCheckProject ->
          with(moduleCheckProject) {
            findings
              .overshotDependencies()
              .mapNotNull { dependency ->
                if (dependency.dependencyPath in alwaysIgnore) {
                  null
                } else {
                  dependency
                }
              }
              .distinctBy { it.dependencyPath }
          }
        }
        .finish()

//               project.moduleCheckProjects()
//                 .sorted()
//                 .filterNot { moduleCheckProject ->
//                   moduleCheckProject.path in ignoreAll
//          }
//                 .flatMap { moduleCheckProject ->
//                   with(moduleCheckProject) {
//                     listOf(
//                   findings.    redundantAndroidTest(),
//                   findings.    redundantMain(),
//                   findings.    redundantTest()
//                     ).flatMap { dependencies ->
//                       dependencies.mapNotNull { dependency ->
//                         if (dependency.dependencyPath in alwaysIgnore) {
//                           null
//                         } else {
//                           dependency
//                         }
//                       }
//                     }
//                       .distinctBy { it.position }
//                   }
//                 }
//                 .finish()
//
//               project.moduleCheckProjects()
//                 .sorted()
//                 .filterNot { moduleCheckProject ->
//                   moduleCheckProject.path in ignoreAll
//          }
//                 .flatMap { moduleCheckProject ->
//                   with(moduleCheckProject) {
//                     listOf(
//                    findings.  unusedAndroidTest(),
//                    findings.  unusedApi(),
//                    findings.  unusedCompileOnly(),
//                    findings.  unusedImplementation(),
//                    findings.  unusedTestImplementation()
//                     ).flatMap { dependencies ->
//                       dependencies.mapNotNull { dependency ->
//                         if (dependency.dependencyPath in alwaysIgnore) {
//                           null
//                         } else {
//                           dependency
//                         }
//                       }
//                     }
//                       .distinctBy { it.position }
//                   }
//                 }
//                 .finish()
    }

    project.moduleCheckProjects().groupBy { it.findings.getMainDepth() }.toSortedMap()
      .forEach { (depth, modules) ->
        cli.printBlue("""$depth  ${modules.joinToString { it.path }}""")
      }

    cli.printGreen("total parsing time --> $time milliseconds")
  }

  private fun List<DependencyFinding>.finish() {

    forEach { finding ->
      logger.error(
        "${finding.problemName} ${finding.configurationName} dependency: ${finding.logString()}"
      )
      finding.fix()
//      ModuleCheckProject.reset()
    }
  }

  //    val byProject = groupBy { it.dependentProject }
  //
  //    val sortedProjects = byProject.keys.sorted()
  //
  //    sortedProjects.forEach { key ->
  //      val forProject = byProject.getValue(key).groupBy { it::class }
  //
  //      listOf(
  //        DependencyFinding.OverShotDependency::class,
  //        DependencyFinding.RedundantDependency::class,
  //        DependencyFinding.UnusedDependency::class
  //      ).forEach { type ->
  //        forProject[type]?.forEach { finding ->
  //          logger.error("${finding.problemName} ${finding.configurationName} dependency:
  // ${finding.logString()}")
  //          finding.fix()
  //        }
  //      }
  //
  //    }
  //  }
}
