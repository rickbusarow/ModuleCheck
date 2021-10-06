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

import modulecheck.api.*
import modulecheck.gradle.GradleProjectProvider
import modulecheck.gradle.ModuleCheckExtension
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

abstract class ModuleCheckTask :
  DefaultTask(),
  ProjectsAware {

  init {
    group = "moduleCheck"
  }

  @get:Input
  val settings: ModuleCheckExtension = project.extensions.getByType()

  @get:Input
  val autoCorrect: Boolean = settings.autoCorrect

  @get:Input
  val deleteUnused: Boolean = settings.deleteUnused

  @get:Input
  final override val projectCache = ConcurrentHashMap<String, Project2>()

  @get:Input
  val projectProvider = GradleProjectProvider(project.rootProject, projectCache)

  @get:Input
  val logger = GradleLogger(project)

  @TaskAction
  fun evaluate() {
    val results = measured {
      project
        .allprojects
        .filter { it.buildFile.exists() }
        .filterNot { it.path in settings.doNotCheck }
        .map { projectProvider.get(it.path) }
        .getFindings()
        .distinct()
    }

    val numIssues = results.finish()

    if (numIssues > 0) {
      throw GradleException("ModuleCheck found $numIssues issues which were not auto-corrected.")
    }
  }

  abstract fun List<Project2>.getFindings(): List<Finding>

  private fun TimedResults<List<Finding>>.finish(): Int {
    val grouped = data.groupBy { it.dependentPath }

    @Suppress("MagicNumber")
    val secondsDouble = timeMillis / 1000.0

    if (data.isNotEmpty()) {
      logger.printSuccessHeader("ModuleCheck found ${data.size} issues in $secondsDouble seconds\n")
    }

    val unFixed = grouped
      .entries
      .sortedBy { it.key }
      .flatMap { (path, list) ->

        logger.printHeader("\t$path")

        val logStrings = mutableMapOf<Finding, String>()

        val (fixed, toFix) = list.partition { finding ->

          logStrings[finding] = finding.logString()

          if (!autoCorrect) return@partition false

          if (deleteUnused && finding is Deletable) {
            finding.delete()
          } else {
            (finding as? Fixable)?.fix() ?: false
          }
        }

        fixed.forEach { finding ->
          logger.printWarning("\t\t${logStrings.getValue(finding)}")
        }

        toFix.forEach { finding ->
          logger.printFailure("\t\t${logStrings.getValue(finding)}")
        }

        toFix
      }

    return unFixed.size
  }

  inline fun <T, R> T.measured(action: T.() -> R): TimedResults<R> {
    var r: R

    val time = measureTimeMillis {
      r = action()
    }

    return TimedResults(time, r)
  }

  data class TimedResults<R>(val timeMillis: Long, val data: R)
}
