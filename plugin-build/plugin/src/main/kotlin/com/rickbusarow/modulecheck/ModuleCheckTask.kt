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

  fun Project.moduleCheckProjects() = project.rootProject.allprojects
    .filter { gradleProject -> gradleProject.buildFile.exists() }
    .map { gradleProject ->
      ModuleCheckProject.from(gradleProject)
    }

  @TaskAction
  fun execute() = runBlocking {
    val cli = Cli()


    val alwaysIgnore = alwaysIgnore.get()
    val ignoreAll = ignoreAll.get()

    val time = measureTimeMillis {

      project.moduleCheckProjects()
        .sorted()
        .filterNot { moduleCheckProject -> ignoreAll.contains(moduleCheckProject.path) }
        .flatMap { moduleCheckProject ->
          with(moduleCheckProject) {
            listOf(
              overshotDependencies()
            ).flatMap { dependencies ->
              dependencies.mapNotNull { dependency ->
                if (alwaysIgnore.contains(dependency.dependencyPath)) {
                  null
                } else {
                  dependency
                }
              }
            }
              .distinctBy { it.dependencyPath }
          }
        }
        .finish()

//      project.moduleCheckProjects()
    //      .sorted()
//        .filterNot { moduleCheckProject -> ignoreAll.contains(moduleCheckProject.path) }
//        .flatMap { moduleCheckProject ->
//          with(moduleCheckProject) {
//            listOf(
//              redundantAndroidTest(),
//              redundantMain(),
//              redundantTest()
//            ).flatMap { dependencies ->
//              dependencies.mapNotNull { dependency ->
//                if (alwaysIgnore.contains(dependency.dependencyPath)) {
//                  null
//                } else {
//                  dependency
//                }
//              }
//            }
//              .distinctBy { it.position }
//          }
//        }
//        .finish()
//
//    project.moduleCheckProjects()
    //    .sorted()
//        .filterNot { moduleCheckProject -> ignoreAll.contains(moduleCheckProject.path) }
//        .flatMap { moduleCheckProject ->
//          with(moduleCheckProject) {
//            listOf(
//              unusedAndroidTest(),
//              unusedApi(),
//              unusedCompileOnly(),
//              unusedImplementation(),
//              unusedTestImplementation()
//            ).flatMap { dependencies ->
//              dependencies.mapNotNull { dependency ->
//                if (alwaysIgnore.contains(dependency.dependencyPath)) {
//                  null
//                } else {
//                  dependency
//                }
//              }
//            }
//              .distinctBy { it.position }
//          }
//        }
//        .finish()
    }

    project.moduleCheckProjects()
      .groupBy { it.getMainDepth() }
      .toSortedMap()
      .forEach { (depth, modules) ->
        cli.printBlue("""$depth  ${modules.joinToString { it.path }}""")
      }

    cli.printGreen("total parsing time --> $time milliseconds")

  }

  private fun List<DependencyFinding>.finish() {

    forEach { finding ->
      logger.error("${finding.problemName} ${finding.configurationName} dependency: ${finding.logString()}")
      finding.fix()
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
//          logger.error("${finding.problemName} ${finding.configurationName} dependency: ${finding.logString()}")
//          finding.fix()
//        }
//      }
//
//    }
  }
}
