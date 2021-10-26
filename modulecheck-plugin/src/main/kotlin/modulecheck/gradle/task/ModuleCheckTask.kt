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

import modulecheck.api.Deletable
import modulecheck.api.Finding
import modulecheck.api.Finding.LogElement
import modulecheck.api.Fixable
import modulecheck.gradle.GradleProjectProvider
import modulecheck.gradle.ModuleCheckExtension
import modulecheck.parsing.Project2
import modulecheck.parsing.ProjectsAware
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
        .filterNot { it.shouldSkip() }
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
      logger.printSuccessHeader(
        "ModuleCheck found ${data.size} issues in $secondsDouble seconds\n" +
          "To ignore any of these findings, annotate the dependency declaration.\n" +
          "For Kotlin files, use @Suppress(\"<the name of the issue>\") or " +
          "@SuppressWarnings(\"<the name of the ID>\").\n" +
          "For Groovy files, add a comment above the declaration: " +
          "//noinspection <the name of the issue>."
      )
    }

    val unFixed = grouped
      .entries
      .sortedBy { it.key }
      .map { (path, list) ->

        logger.printHeader("${tab(1)}$path")

        val elements = list
          .map { finding ->

            finding.logElement().apply {

              fixed = when {
                !autoCorrect -> false
                deleteUnused && finding is Deletable -> {
                  finding.delete()
                }
                else -> {
                  (finding as? Fixable)?.fix() ?: false
                }
              }
            }
          }

        if (elements.isEmpty()) return@map 0

        val maxDependencyPath = maxOf(
          elements.maxOf { it.dependencyPath.length },
          "dependency".length
        )
        val maxProblemName = elements.maxOf { it.problemName.length }
        val maxSource = maxOf(elements.maxOf { it.sourceOrNull.orEmpty().length }, "source".length)
        val maxFilePathStr = elements.maxOf { it.filePathStr.length }

        logger.printHeader(
          tab(2) +
            "dependency".padEnd(maxDependencyPath) +
            tab(1) +
            "name".padEnd(maxProblemName) +
            tab(1) +
            "source".padEnd(maxSource) +
            tab(1) +
            "build file".padEnd(maxFilePathStr)
        )

        elements.sortedWith(
          compareBy(
            { !it.fixed },
            { it.positionOrNull }
          )
        ).forEach { logElement ->

          logElement.log(
            tab(2) +
              logElement.dependencyPath.padEnd(maxDependencyPath) +
              tab(1) +
              logElement.problemName.padEnd(maxProblemName) +
              tab(1) +
              logElement.sourceOrNull.orEmpty().padEnd(maxSource) +
              tab(1) +
              logElement.filePathStr.padEnd(maxFilePathStr)
          )
        }

        elements.count { !it.fixed }
      }

    return unFixed.sum()
  }

  inline fun <T, R> T.measured(action: T.() -> R): TimedResults<R> {
    var r: R

    val time = measureTimeMillis {
      r = action()
    }

    return TimedResults(time, r)
  }

  data class TimedResults<R>(val timeMillis: Long, val data: R)

  private fun tab(numTabs: Int) = "    ".repeat(numTabs)

  private fun LogElement.log(message: String) {
    if (fixed) {
      logger.printWarning(message)
    } else {
      logger.printFailure(message)
    }
  }
}
