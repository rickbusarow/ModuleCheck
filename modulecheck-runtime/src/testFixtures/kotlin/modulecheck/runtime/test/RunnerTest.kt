/*
 * Copyright (C) 2021-2022 Rick Busarow
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

package modulecheck.runtime.test

import dispatch.core.DispatcherProvider
import io.kotest.assertions.asClue
import kotlinx.coroutines.runBlocking
import modulecheck.api.finding.Finding
import modulecheck.api.finding.FindingFactory
import modulecheck.api.finding.FindingResultFactory
import modulecheck.api.finding.RealFindingResultFactory
import modulecheck.api.rule.RuleFactory
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.api.test.ReportingLogger
import modulecheck.api.test.TestSettings
import modulecheck.core.rule.ModuleCheckRuleFactory
import modulecheck.core.rule.MultiRuleFindingFactory
import modulecheck.project.Logger
import modulecheck.project.McProject
import modulecheck.project.ProjectProvider
import modulecheck.project.test.ProjectTest
import modulecheck.reporting.checkstyle.CheckstyleReporter
import modulecheck.reporting.console.ReportFactory
import modulecheck.reporting.graphviz.GraphvizFactory
import modulecheck.reporting.graphviz.GraphvizFileWriter
import modulecheck.runtime.ModuleCheckRunner

abstract class RunnerTest : ProjectTest() {

  open val settings: ModuleCheckSettings by resets { TestSettings() }
  open val logger: ReportingLogger by resets { ReportingLogger() }

  open val ruleFactory: RuleFactory by resets { ModuleCheckRuleFactory() }
  open val findingFactory: FindingFactory<Finding> by resets {
    MultiRuleFindingFactory(
      settings,
      ruleFactory.create(settings)
    )
  }

  @Suppress("LongParameterList")
  fun run(
    autoCorrect: Boolean = true,
    strictResolution: Boolean = false,
    findingFactory: FindingFactory<out Finding> = this.findingFactory,
    settings: ModuleCheckSettings = this.settings,
    logger: Logger = this.logger,
    projectProvider: ProjectProvider = this.projectProvider,
    findingResultFactory: FindingResultFactory = RealFindingResultFactory(),
    reportFactory: ReportFactory = ReportFactory(),
    checkstyleReporter: CheckstyleReporter = CheckstyleReporter(),
    graphvizFileWriter: GraphvizFileWriter = GraphvizFileWriter(settings, GraphvizFactory()),
    dispatcherProvider: DispatcherProvider = DispatcherProvider()
  ): Result<Unit> = runBlocking {

    "Resolving all references BEFORE running ModuleCheck".asClue {
      if (strictResolution) {
        resolveReferences()
      }
    }

    val result = ModuleCheckRunner(
      autoCorrect = autoCorrect,
      settings = settings,
      findingFactory = findingFactory,
      logger = logger,
      findingResultFactory = findingResultFactory,
      reportFactory = reportFactory,
      checkstyleReporter = checkstyleReporter,
      graphvizFileWriter = graphvizFileWriter,
      dispatcherProvider = dispatcherProvider,
      projectProvider = projectProvider
    ).run(allProjects())

    if (autoCorrect) {

      // Re-parse everything from scratch to ensure that auto-correct didn't break anything.
      projectCache.clearContexts()

      "Resolving all references after auto-correct\n".asClue {
        if (strictResolution) {
          resolveReferences()
        }
      }
    }

    return@runBlocking result
  }

  fun findingFactory(
    fixable: List<Finding> = emptyList(),
    sorts: List<Finding> = emptyList(),
    reports: List<Finding> = emptyList()
  ): FindingFactory<*> = object : FindingFactory<Finding> {
    override suspend fun evaluateFixable(projects: List<McProject>): List<Finding> = fixable
    override suspend fun evaluateSorts(projects: List<McProject>): List<Finding> = sorts
    override suspend fun evaluateReports(projects: List<McProject>): List<Finding> = reports
  }

  fun ReportingLogger.parsedReport(): List<Pair<String, List<ProjectFindingReport>>> {
    return collectReport().joinToString()
      .clean()
      .parseReportOutput()
  }
}
