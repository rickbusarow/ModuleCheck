package com.rickbusarow.modulecheck

import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType

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

  }
}



