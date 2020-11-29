package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.internal.Cli
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import kotlin.system.measureTimeMillis

abstract class ModuleCheckTask : DefaultTask() {

  init {
    description = "verification"
  }

  @TaskAction
  fun execute() {
    val cli = Cli()

    lateinit var things: List<ModuleCheckProject.JavaModuleCheckProject>

    val time = measureTimeMillis {

      things = project.rootProject.allprojects
        .map { gradleProject ->
          gradleProject.toModuleCheckProject()
            .also { moduleCheckProject -> moduleCheckProject.init() }
        }
    }

    val mapped: Map<Project, ModuleCheckProject.JavaModuleCheckProject> =
      things.associateBy { it.project }

    val unused = things.flatMap { parent ->

      parent.mainDependencies.mapNotNull { projectDependency ->

        val moduleCheckProject = mapped[projectDependency]

        require(moduleCheckProject != null) {
          """map does not contain ${projectDependency} 
              |
              |${mapped.keys}
            """.trimMargin()
        }

        val used = parent.mainImports.any { importString ->
          when {
            moduleCheckProject.mainPackages.contains(importString) -> true
            else -> parent.mainDependencies.any { childProjectDependency ->
              val dpp = mapped.getValue(childProjectDependency)

              dpp.mainDependencies.contains(projectDependency).also {
                if (it) {
                  logger.info("project ${parent.path} couldn't find import for $projectDependency but allowing because it's a valid dependency for child ${childProjectDependency}")
                }
              }
            }
          }

        }

        if (!used) {
          parent.project to projectDependency.path
        } else null

      }
    }

    things.groupBy { it.depth }.toSortedMap().forEach { (depth, modules) ->
      cli.printBlue("""$depth  ${modules.joinToString { it.path }}""")
    }

    cli.printGreen("total parsing time --> $time milliseconds")

    if (unused.isNotEmpty()) {

      unused.forEach {
        logger.error("unused dependency: ${it.first.buildFile} -- ${it.second}")
//            logger.error("unused dependency: ${it.first.projectDir}/build.gradle.kts: (15, 1): ${it.second}")
      }
    }
  }
}
