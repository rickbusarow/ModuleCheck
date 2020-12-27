package com.rickbusarow.modulecheck

import kotlinx.coroutines.runBlocking
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
  fun execute()  = runBlocking {
    val alwaysIgnore = alwaysIgnore.get()
    val ignoreAll = ignoreAll.get()

    measured {
      val unused = UnusedKaptRule(project, alwaysIgnore, ignoreAll).get()

      unused
        .forEach { finding ->

        project.logger.error(
          "unused ${finding.config.name} dependency: ${finding.logString()}"
        )
        finding.fix()
//      MCP.reset()
      }
    }
  }

}
