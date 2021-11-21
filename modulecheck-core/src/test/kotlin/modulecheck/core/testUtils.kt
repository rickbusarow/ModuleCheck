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

package modulecheck.core

import dispatch.core.DispatcherProvider
import modulecheck.api.*
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.reporting.checkstyle.CheckstyleReporter
import modulecheck.reporting.console.ReportFactory
import modulecheck.reporting.graphviz.GraphvizFactory
import modulecheck.reporting.graphviz.GraphvizFileWriter
import modulecheck.runtime.ModuleCheckRunner

fun ModuleCheckRunner(
  autoCorrect: Boolean,
  settings: ModuleCheckSettings,
  findingFactory: FindingFactory<out Finding>,
  logger: Logger,
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
  dispatcherProvider = dispatcherProvider
)
