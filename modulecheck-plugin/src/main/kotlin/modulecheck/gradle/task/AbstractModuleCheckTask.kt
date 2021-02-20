/*
 * Copyright (C) 2021 Rick Busarow
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

package modulecheck.gradle.task

import modulecheck.api.Finding
import modulecheck.api.Fixable
import modulecheck.gradle.ModuleCheckExtension
import modulecheck.gradle.internal.Output
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import java.util.*
import kotlin.system.measureTimeMillis

abstract class AbstractModuleCheckTask : DefaultTask() {

  init {
    group = "moduleCheck"
  }

  @get:Input
  val extension: ModuleCheckExtension = project.extensions.getByType()

  @get:Input
  val autoCorrect: Boolean = extension.autoCorrect

  @get:Input
  val alwaysIgnore: Set<String> = extension.alwaysIgnore

  @get:Input
  val ignoreAll: Set<String> = extension.ignoreAll

  @TaskAction
  fun evaluate() {
    val numIssues = getFindings().finish()

    if (numIssues > 0) {
      throw GradleException("\"ModuleCheck found $numIssues issues")
    }
  }

  protected abstract fun getFindings(): List<Finding>

  private fun Collection<Finding>.finish(): Int {
    val grouped = this.groupBy { it.buildFile }

    Output.printMagenta("ModuleCheck found ${this.size} issues:\n")

    val unFixed = grouped
      .entries
      .sortedBy { it.key.path }
      .flatMap { (project, list) ->

        Output.printMagenta("\t${project.path}")

        val (fixed, toFix) = list.partition { finding ->
          autoCorrect && (finding as? Fixable)?.fix() ?: false
        }

        fixed.forEach { finding ->
          Output.printYellow("\t\t${finding.logString()}")
        }

        toFix.forEach { finding ->
          Output.printRed("\t\t${finding.logString()}")
        }

        toFix
      }

    return unFixed.size
  }

  protected inline fun <T, R> T.measured(action: T.() -> R): R {
    var r: R

    val time = measureTimeMillis {
      r = action()
    }

    Output.printGreen("total parsing time: $time milliseconds")

    return r
  }
}
