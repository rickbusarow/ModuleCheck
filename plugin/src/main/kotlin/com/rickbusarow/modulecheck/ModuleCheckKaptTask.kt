package com.rickbusarow.modulecheck

import org.gradle.api.tasks.TaskAction

/**
 * Loops through all registered annotation processors for each module,
 * checking that at least one applicable annotation is imported in the source.
 *
 * Throws warnings if a processor is applied without any annotations being used.
 */
abstract class ModuleCheckKaptTask : AbstractModuleCheckTask() {

  init {
    description =
      "Checks all modules with registered annotation processors to ensure they're needed."
  }

  @TaskAction
  fun check() {
    val messages = project
      .moduleCheckProjects()
      .map { mcp ->

        val usedMain = mcp.kaptDependencies.main.filter {
          it.annotationImports.any { annotationRegex ->
            mcp.mainImports.any { imp ->
              annotationRegex.matches(imp)
            }
          }
        }

        val unusedMain = mcp.kaptDependencies.main.filter {
          it.annotationImports.none { annotationRegex ->
            mcp.mainImports.any { imp ->
              annotationRegex.matches(imp)
            }
          }
        }

        val usedTest = mcp.kaptDependencies.test.filter {
          it.annotationImports.any { annotationRegex ->
            mcp.testImports.any { imp ->
              annotationRegex.matches(imp)
            }
          }
        }

        val unusedTest = mcp.kaptDependencies.test.filter {
          it.annotationImports.none { annotationRegex ->
            mcp.testImports.any { imp ->
              annotationRegex.matches(imp)
            }
          }
        }

        """project ------> ${mcp.path}
            |
            |androidTest -- ${mcp.kaptDependencies.androidTest}
            |
            |used main -- $usedMain
            |unused main -- $unusedMain
            |
            |used test -- $usedTest
            |unused test -- $unusedTest
            |
            |
            |
            |
          """.trimMargin()
      }

    messages.forEach {
      project.logger.warn(it + "\n")
    }
  }
}
