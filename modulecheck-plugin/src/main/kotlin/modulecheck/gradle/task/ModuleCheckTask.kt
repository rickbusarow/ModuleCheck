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
import modulecheck.api.Finding.FindingResult
import modulecheck.api.FindingFactory
import modulecheck.api.FindingProcessor
import modulecheck.api.RealFindingProcessor
import modulecheck.gradle.GradleProjectProvider
import modulecheck.gradle.ModuleCheckExtension
import modulecheck.parsing.Project2
import modulecheck.parsing.ProjectsAware
import modulecheck.reporting.checkstyle.CheckstyleReporter
import modulecheck.reporting.console.LoggingReporter
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

abstract class ModuleCheckTask<T : Finding> :
  DefaultTask(),
  ProjectsAware,
  FindingFactory<T>,
  FindingProcessor by RealFindingProcessor() {

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

  @get:Input
  val loggingReporter = LoggingReporter(logger)

  @TaskAction
  fun run() {

    var totalIssues = 0

    val results = measured {
      project
        .allprojects
        .filter { it.buildFile.exists() }
        .filterNot { it.path in settings.doNotCheck }
        .map { projectProvider.get(it.path) }
        .evaluate()
        .distinct()
        .filterNot { it.shouldSkip() }
        .also { totalIssues = it.size }
        .finish()
    }

    val numIssues = results.data

    @Suppress("MagicNumber")
    val secondsDouble = results.timeMillis / 1000.0

    if (totalIssues > 0) {
      logger.printInfo(
        "ModuleCheck found $totalIssues issues in $secondsDouble seconds.\n\n" +
          "To ignore any of these findings, annotate the dependency declaration with " +
          "@Suppress(\"<the name of the issue>\") in Kotlin, or " +
          "//noinspection <the name of the issue> in Groovy.\n" +
          "See https://rbusarow.github.io/ModuleCheck/docs/suppressing-findings for more info."
      )
    }

    if (numIssues > 0) {
      throw GradleException("ModuleCheck found $numIssues issues which were not auto-corrected.")
    }
  }

  private fun List<T>.finish(): Int {

    val results = groupBy { it.dependentPath.lowercase(Locale.getDefault()) }
      .flatMap { (_, list) ->

        list.toResults(
          autoCorrect = autoCorrect,
          deleteUnused = deleteUnused
        )
      }

    loggingReporter.reportResults(results)

    val xmlString = CheckstyleReporter().reportResults(results)

    project.rootProject.buildDir.let {
      val mc = File(it, "moduleCheck").also { it.mkdirs() }

      File(mc, "report.xml").writeText(xmlString)
    }

    return results.count { !it.fixed }
  }

  private inline fun <T, R> T.measured(action: T.() -> R): TimedResults<R> {
    var r: R

    val time = measureTimeMillis {
      r = action()
    }

    return TimedResults(time, r)
  }

  data class TimedResults<R>(val timeMillis: Long, val data: R)

  private fun tab(numTabs: Int) = "    ".repeat(numTabs)

  private fun FindingResult.log(message: String) {
    if (fixed) {
      logger.printWarningLine(message)
    } else {
      logger.printFailureLine(message)
    }
  }
}
