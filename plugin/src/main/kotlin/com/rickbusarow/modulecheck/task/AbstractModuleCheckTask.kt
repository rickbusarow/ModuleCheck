/*
 * Copyright (C) 2020 Rick Busarow
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rickbusarow.modulecheck.task

import com.rickbusarow.modulecheck.Finding
import com.rickbusarow.modulecheck.Fixable
import com.rickbusarow.modulecheck.MCP
import com.rickbusarow.modulecheck.ModuleCheckExtension
import com.rickbusarow.modulecheck.internal.Output
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.getByType
import kotlin.system.measureTimeMillis

abstract class AbstractModuleCheckTask : DefaultTask() {

  init {
    group = "moduleCheck"
  }

  @get:Input
  val autoCorrect: Property<Boolean> = project.extensions
    .getByType<ModuleCheckExtension>()
    .autoCorrect

  @get:Input
  val alwaysIgnore: SetProperty<String> = project.extensions
    .getByType<ModuleCheckExtension>()
    .alwaysIgnore

  @get:Input
  val ignoreAll: SetProperty<String> = project.extensions
    .getByType<ModuleCheckExtension>()
    .ignoreAll

  protected fun List<Finding>.finish() {
    val grouped = this.groupBy { it.dependentProject }

    Output.printMagenta("ModuleCheck found ${this.size} issues:\n")

    grouped.forEach { (project, list) ->
      Output.printMagenta("\t${project.path}")

      list.forEach { finding ->

        Output.printYellow("\t\t${finding.logString()}")

        if (finding is Fixable && autoCorrect.get()) {
          finding.fix()
        }
      }
    }
  }

  protected fun Project.moduleCheckProjects() =
    project.rootProject.allprojects
      .filter { gradleProject -> gradleProject.buildFile.exists() }
      .map { gradleProject -> MCP.from(gradleProject) }

  protected inline fun <T, R> T.measured(action: T.() -> R): R {
    var r: R

    val time = measureTimeMillis {
      r = action()
    }

    Output.printGreen("total parsing time --> $time milliseconds")

    return r
  }
}
