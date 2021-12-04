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

package modulecheck.runtime.test

import dispatch.core.DispatcherProvider
import modulecheck.api.finding.Finding
import modulecheck.api.finding.FindingFactory
import modulecheck.api.finding.FindingResultFactory
import modulecheck.api.finding.RealFindingResultFactory
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.api.test.ReportingLogger
import modulecheck.api.test.TestSettings
import modulecheck.parsing.ProjectProvider
import modulecheck.project.Logger
import modulecheck.project.McProject
import modulecheck.project.test.ProjectTest
import modulecheck.reporting.checkstyle.CheckstyleReporter
import modulecheck.reporting.console.ReportFactory
import modulecheck.reporting.graphviz.GraphvizFactory
import modulecheck.reporting.graphviz.GraphvizFileWriter
import modulecheck.runtime.ModuleCheckRunner

abstract class RunnerTest : ProjectTest() {

  open val settings by resets { TestSettings() }
  open val logger by resets { ReportingLogger() }

  @Suppress("LongParameterList")
  fun runner(
    autoCorrect: Boolean,
    findingFactory: FindingFactory<out Finding>,
    settings: ModuleCheckSettings = this.settings,
    logger: Logger = this.logger,
    projectProvider: ProjectProvider = this.projectProvider,
    findingResultFactory: FindingResultFactory = RealFindingResultFactory(),
    reportFactory: ReportFactory = ReportFactory(),
    checkstyleReporter: CheckstyleReporter = CheckstyleReporter(),
    graphvizFileWriter: GraphvizFileWriter = GraphvizFileWriter(settings, GraphvizFactory()),
    dispatcherProvider: DispatcherProvider = DispatcherProvider()
  ): ModuleCheckRunner = ModuleCheckRunner(
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
  )

  fun findingFactory(
    fixable: List<Finding> = emptyList(),
    sorts: List<Finding> = emptyList(),
    reports: List<Finding> = emptyList()
  ): FindingFactory<*> = object : FindingFactory<Finding> {
    override suspend fun evaluateFixable(projects: List<McProject>): List<Finding> = fixable
    override suspend fun evaluateSorts(projects: List<McProject>): List<Finding> = sorts
    override suspend fun evaluateReports(projects: List<McProject>): List<Finding> = reports
  }
}
