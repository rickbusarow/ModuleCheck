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

    lateinit var moduleCheckProjects: List<ModuleCheckProject.JavaModuleCheckProject>
    lateinit var mainUnused: List<UnusedDependency>
    lateinit var testUnused: List<UnusedDependency>

    val time = measureTimeMillis {

      moduleCheckProjects = project.rootProject.allprojects
        .map { gradleProject ->
          gradleProject.toModuleCheckProject()
            .also { moduleCheckProject -> moduleCheckProject.init() }
        }

      val mapped  = moduleCheckProjects.associateBy { it.project }

      mainUnused = moduleCheckProjects.flatMap { parent ->

        parent.mainDependencies.mapNotNull { projectDependency ->

          val moduleCheckProject = mapped[projectDependency.project]

          require(moduleCheckProject != null) {
            """map does not contain $projectDependency 
              |
              |${mapped.keys}
            """.trimMargin()
          }

          val used = parent.mainImports.any { importString ->
            when {
              moduleCheckProject.mainPackages.contains(importString) -> true
              else -> parent.mainDependencies.any { childProjectDependency ->
                val dpp = mapped.getValue(childProjectDependency.project)

                dpp.mainDependencies.contains(projectDependency).also {
                  if (it) {
                    logger.info(
                      "project ${parent.path} couldn't find import for $projectDependency but" +
                          " allowing because it's a valid dependency for child $childProjectDependency"
                    )
                  }
                }
              }
            }

          }

          if (!used) {
            UnusedDependency(parent.project, projectDependency.position, projectDependency.project.path)
          } else null

        }
      }

      testUnused = moduleCheckProjects.flatMap { parent ->

        parent.testDependencies.mapNotNull { projectDependency ->

          val moduleCheckProject = mapped[projectDependency.project]

          require(moduleCheckProject != null) {
            """map does not contain $projectDependency 
              |
              |${mapped.keys}
            """.trimMargin()
          }

          val used = parent.testImports.any { importString ->
            when {
              moduleCheckProject.mainPackages.contains(importString) -> true
              else -> parent.testDependencies.any { childProjectDependency ->
                val dpp = mapped.getValue(childProjectDependency.project)

                dpp.testDependencies.contains(projectDependency).also {
                  if (it) {
                    logger.info(
                      "project ${parent.path} couldn't find import for $projectDependency but" +
                          " allowing because it's a valid dependency for child $childProjectDependency"
                    )
                  }
                }
              }
            }

          }

          if (!used) {
            UnusedDependency(parent.project, projectDependency.position, projectDependency.project.path)
          } else null

        }
      }
    }

    moduleCheckProjects.groupBy { it.depth }.toSortedMap().forEach { (depth, modules) ->
      cli.printBlue("""$depth  ${modules.joinToString { it.path }}""")
    }

    cli.printGreen("total parsing time --> $time milliseconds")

    if (mainUnused.isNotEmpty()) {

      mainUnused.forEach {
        logger.error("unused main dependency: ${it.logString()}")
      }
    }

    if (testUnused.isNotEmpty()) {

      testUnused.forEach {
        logger.error("unused test dependency: ${it.logString()}")
      }
    }
  }
}
