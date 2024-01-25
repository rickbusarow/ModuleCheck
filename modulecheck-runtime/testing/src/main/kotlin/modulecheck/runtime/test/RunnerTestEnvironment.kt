/*
 * Copyright (C) 2021-2024 Rick Busarow
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

import com.rickbusarow.kase.ParamTestEnvironmentFactory
import com.rickbusarow.kase.files.TestLocation
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

/**
 * Defines a test environment which uses a
 * [ModuleCheckRunner][modulecheck.runtime.ModuleCheckRunner].
 *
 * @property projectCache An instance of [ProjectCache].
 * @property logger A [ReportingLogger] for logging reporting events.
 * @property ruleFilter A [RuleFilter] for filtering out unwanted rules.
 * @property settings A function to generate [ModuleCheckSettings].
 * @property codeGeneratorBindings A function to generate a list of [CodeGeneratorBinding].
 * @property rules A function to generate a list of [ModuleCheckRule].
 * @property findingFactory A function to generate a [FindingFactory].
 */
data class RunnerTestEnvironmentParams(
  val projectCache: ProjectCache,
  val logger: ReportingLogger,
  val ruleFilter: RuleFilter,
  val settings: (RunnerTestEnvironment) -> ModuleCheckSettings,
  val codeGeneratorBindings: (ModuleCheckSettings) -> List<CodeGeneratorBinding>,
  val rules: (ModuleCheckSettings, RuleFilter) -> List<ModuleCheckRule<*>>,
  val findingFactory: (List<ModuleCheckRule<*>>) -> FindingFactory<out Finding>
) : TestEnvironmentParams

class RunnerTestEnvironmentFactory : ParamTestEnvironmentFactory<RunnerTestEnvironmentParams, RunnerTestEnvironment> {
  override fun create(
    params: RunnerTestEnvironmentParams,
    names: List<String>,
    location: TestLocation
  ): RunnerTestEnvironment = RunnerTestEnvironment(
    projectCache = params.projectCache,
    logger = params.logger,
    ruleFilter = params.ruleFilter,
    settings = params.settings,
    codeGeneratorBindings = params.codeGeneratorBindings,
    rules = params.rules,
    findingFactory = params.findingFactory,
    names = names,
    testLocation = location
  )
}

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
 * @param names Array of variant names for the test.
 * @param testLocation details about the actual test function, so that we can
 *   get the test name. This must be grabbed as soon as possible, since default
 *   functions, inline functions, sequences, and iterators all redirect things
 *   and have a chance of hiding the original calling function completely.
 */
open class RunnerTestEnvironment(
  override val projectCache: ProjectCache,
  val logger: ReportingLogger,
  val ruleFilter: RuleFilter,
  settings: (RunnerTestEnvironment) -> ModuleCheckSettings,
  codeGeneratorBindings: (ModuleCheckSettings) -> List<CodeGeneratorBinding>,
  rules: (ModuleCheckSettings, RuleFilter) -> List<ModuleCheckRule<*>>,
  findingFactory: (List<ModuleCheckRule<*>>) -> FindingFactory<out Finding>,
  names: List<String>,
  testLocation: TestLocation
) : ProjectTestEnvironment(
  projectCache = projectCache,
  names = names,
  testLocation = testLocation
) {

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
