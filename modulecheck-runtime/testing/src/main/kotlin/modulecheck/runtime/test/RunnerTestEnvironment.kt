/*
 * Copyright (C) 2021-2023 Rick Busarow
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

import modulecheck.config.CodeGeneratorBinding
import modulecheck.config.ModuleCheckSettings
import modulecheck.finding.Finding
import modulecheck.project.ProjectCache
import modulecheck.project.test.ProjectTestEnvironment
import modulecheck.reporting.logging.test.ReportingLogger
import modulecheck.rule.FindingFactory
import modulecheck.rule.ModuleCheckRule
import modulecheck.rule.RuleFilter
import modulecheck.testing.TestEnvironmentParams
import modulecheck.testing.clean
import java.lang.StackWalker.StackFrame

data class RunnerTestEnvironmentParams(
  val projectCache: ProjectCache,
  val logger: ReportingLogger,
  val ruleFilter: RuleFilter,
  val settings: (RunnerTestEnvironment) -> ModuleCheckSettings,
  val codeGeneratorBindings: (ModuleCheckSettings) -> List<CodeGeneratorBinding>,
  val rules: (ModuleCheckSettings, RuleFilter) -> List<ModuleCheckRule<*>>,
  val findingFactory: (List<ModuleCheckRule<*>>) -> FindingFactory<out Finding>,
  override val testStackFrame: StackFrame,
  override val testVariantNames: List<String>
) : TestEnvironmentParams

/**
 * Defines a test environment which uses a
 * [ModuleCheckRunner][modulecheck.runtime.ModuleCheckRunner].
 *
 * @property projectCache An instance of [ProjectCache].
 * @property logger A [ReportingLogger] for logging reporting events.
 * @property ruleFilter A [RuleFilter] for filtering out unwanted rules.
 * @param settings A function to generate [ModuleCheckSettings].
 * @param codeGeneratorBindings A function to generate a list of [CodeGeneratorBinding].
 * @param rules A function to generate a list of [ModuleCheckRule].
 * @param findingFactory A function to generate a [FindingFactory].
 * @param testStackFrame A stack frame representing the test location.
 * @param testVariantNames Array of variant names for the test.
 */
open class RunnerTestEnvironment(
  override val projectCache: ProjectCache,
  val logger: ReportingLogger,
  val ruleFilter: RuleFilter,
  settings: (RunnerTestEnvironment) -> ModuleCheckSettings,
  codeGeneratorBindings: (ModuleCheckSettings) -> List<CodeGeneratorBinding>,
  rules: (ModuleCheckSettings, RuleFilter) -> List<ModuleCheckRule<*>>,
  findingFactory: (List<ModuleCheckRule<*>>) -> FindingFactory<out Finding>,
  testStackFrame: StackWalker.StackFrame,
  testVariantNames: List<String>
) : ProjectTestEnvironment(
  projectCache = projectCache,
  testStackFrame = testStackFrame,
  testVariantNames = testVariantNames
) {

  constructor(params: RunnerTestEnvironmentParams) : this(
    projectCache = params.projectCache,
    logger = params.logger,
    ruleFilter = params.ruleFilter,
    settings = params.settings,
    codeGeneratorBindings = params.codeGeneratorBindings,
    rules = params.rules,
    findingFactory = params.findingFactory,
    testStackFrame = params.testStackFrame,
    testVariantNames = params.testVariantNames
  )

  val settings: ModuleCheckSettings by lazy { settings.invoke(this) }
  override val codeGeneratorBindings: List<CodeGeneratorBinding> by lazy {
    codeGeneratorBindings(this.settings)
  }

  val rules: List<ModuleCheckRule<*>> by lazy { rules.invoke(this.settings, ruleFilter) }

  val findingFactory: FindingFactory<out Finding> by lazy { findingFactory(this.rules) }

  /**
   * Parses the log report into a list of pairs, each containing
   * a report title and a list of [ProjectFindingReport].
   *
   * @receiver The [ReportingLogger] whose logs are to be parsed.
   * @return A list of pairs, each containing a report title and a list of [ProjectFindingReport].
   */
  fun ReportingLogger.parsedReport(): List<Pair<String, List<ProjectFindingReport>>> {
    return collectReport().joinToString()
      .clean(workingDir)
      .parseReportOutput()
  }
}
