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

import com.github.ajalt.mordant.terminal.Terminal
import dispatch.core.DispatcherProvider
import io.kotest.assertions.asClue
import kotlinx.coroutines.runBlocking
import modulecheck.config.CodeGeneratorBinding
import modulecheck.config.ModuleCheckSettings
import modulecheck.config.fake.TestSettings
import modulecheck.config.internal.defaultCodeGeneratorBindings
import modulecheck.dagger.DaggerList
import modulecheck.finding.Finding
import modulecheck.finding.FindingResultFactory
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.project.ProjectProvider
import modulecheck.project.test.ProjectTest
import modulecheck.project.toTypeSafeProjectPathResolver
import modulecheck.reporting.checkstyle.CheckstyleReporter
import modulecheck.reporting.console.DepthLogFactory
import modulecheck.reporting.console.ReportFactory
import modulecheck.reporting.graphviz.GraphvizFactory
import modulecheck.reporting.graphviz.GraphvizFileWriter
import modulecheck.reporting.logging.McLogger
import modulecheck.reporting.logging.TerminalModule
import modulecheck.reporting.logging.test.ReportingLogger
import modulecheck.reporting.sarif.SarifReportFactory
import modulecheck.rule.FindingFactory
import modulecheck.rule.ModuleCheckRule
import modulecheck.rule.RuleFilter
import modulecheck.rule.impl.FindingFactoryImpl
import modulecheck.rule.impl.RealFindingResultFactory
import modulecheck.rule.test.AllRulesComponent
import modulecheck.runtime.ModuleCheckRunner
import modulecheck.testing.TestEnvironmentParams
import modulecheck.utils.mapToSet
import java.lang.StackWalker.StackFrame

@Suppress("UnnecessaryAbstractClass")
abstract class RunnerTest : ProjectTest<RunnerTestEnvironment>() {

  open val settings: RunnerTestEnvironment.() -> ModuleCheckSettings = { TestSettings() }
  open val logger: () -> ReportingLogger = { ReportingLogger() }

  open val ruleFilter: () -> RuleFilter = { RuleFilter.DEFAULT }

  open val rules: (ModuleCheckSettings, RuleFilter) -> List<ModuleCheckRule<*>> =
    { settings, ruleFilter ->
      AllRulesComponent.create(settings, ruleFilter).allRules
    }
  open val findingFactory: (List<ModuleCheckRule<*>>) -> FindingFactory<out Finding> = { rules ->
    FindingFactoryImpl(rules)
  }
  open val codeGeneratorBindings = { settings: ModuleCheckSettings ->
    settings.additionalCodeGenerators
      .plus(defaultCodeGeneratorBindings())
      .plus(
        @Suppress("DEPRECATION")
        settings.additionalKaptMatchers.mapToSet { it.toCodeGeneratorBinding() }
      )
  }

  open fun newRunnerTestEnvironment(
    projectCache: ProjectCache,
    codeGeneratorBindings: (ModuleCheckSettings) -> List<CodeGeneratorBinding>,
    settings: RunnerTestEnvironment.() -> ModuleCheckSettings,
    logger: ReportingLogger,
    ruleFilter: RuleFilter,
    rules: (ModuleCheckSettings, RuleFilter) -> List<ModuleCheckRule<*>> = this.rules,
    findingFactory: (List<ModuleCheckRule<*>>) -> FindingFactory<out Finding> = this.findingFactory,
    testStackFrame: StackFrame,
    testVariantNames: List<String>
  ): RunnerTestEnvironment = RunnerTestEnvironment(
    projectCache = projectCache,
    logger = logger,
    ruleFilter = ruleFilter,
    settings = settings,
    codeGeneratorBindings = codeGeneratorBindings,
    rules = rules,
    findingFactory = findingFactory,
    testStackFrame = testStackFrame,
    testVariantNames = testVariantNames
  )

  override fun newTestEnvironment(params: TestEnvironmentParams): RunnerTestEnvironment {

    return when (params) {
      is RunnerTestEnvironmentParams -> RunnerTestEnvironment(params)
      else -> RunnerTestEnvironment(
        RunnerTestEnvironmentParams(
          projectCache = ProjectCache(),
          codeGeneratorBindings = codeGeneratorBindings,
          settings = settings,
          logger = logger(),
          ruleFilter = ruleFilter(),
          rules = rules,
          findingFactory = findingFactory,
          testStackFrame = params.testStackFrame,
          testVariantNames = params.testVariantNames
        )
      )
    }
  }

  fun RunnerTestEnvironment.run(
    autoCorrect: Boolean = true,
    strictResolution: Boolean = false,
    findingFactory: FindingFactory<out Finding> = this.findingFactory,
    settings: ModuleCheckSettings = this.settings,
    logger: McLogger = this.logger,
    projectProvider: ProjectProvider = this.projectProvider,
    findingResultFactory: FindingResultFactory = RealFindingResultFactory(),
    terminal: Terminal = TerminalModule.provideTerminal(),
    reportFactory: ReportFactory = ReportFactory(terminal),
    checkstyleReporter: CheckstyleReporter = CheckstyleReporter(),
    graphvizFileWriter: GraphvizFileWriter = GraphvizFileWriter(
      settings = settings,
      graphvizFactory = GraphvizFactory(projectProvider.toTypeSafeProjectPathResolver())
    ),
    dispatcherProvider: DispatcherProvider = DispatcherProvider(),
    rules: DaggerList<ModuleCheckRule<*>> = this.rules
  ): Result<Unit> {

    "Resolving all references BEFORE running ModuleCheck".asClue {
      if (strictResolution) {
        runBlocking { resolveReferences() }
      }
    }

    val result = ModuleCheckRunner(
      settings = settings,
      findingFactory = findingFactory,
      logger = logger,
      findingResultFactory = findingResultFactory,
      reportFactory = reportFactory,
      checkstyleReporter = checkstyleReporter,
      graphvizFileWriter = graphvizFileWriter,
      dispatcherProvider = dispatcherProvider,
      sarifReportFactory = SarifReportFactory(
        websiteUrl = { "https://rbusarow.github.io/ModuleCheck" },
        moduleCheckVersion = { "0.12.1-SNAPSHOT" }
      ) { workingDir },
      depthLogFactoryLazy = { DepthLogFactory(terminal) },
      projectProvider = projectProvider,
      rules = rules,
      autoCorrect = autoCorrect
    )
      .run(allProjects())

    if (autoCorrect) {

      // Re-parse everything from scratch to ensure that auto-correct didn't break anything.
      projectCache.clearContexts()

      "Resolving all references after auto-correct\n".asClue {
        if (strictResolution) {
          runBlocking { resolveReferences() }
        }
      }
    }

    return result
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

  private fun List<Pair<String, List<ProjectFindingReport>>>.sorted() = sortedBy { it.first }
    .map { (path, findings) ->
      path to findings.sortedBy { findingReport ->

        val findingName = findingReport::class.java.simpleName
        val config = findingReport.configuration ?: "-"

        "$findingName$config${findingReport.position}"
      }
    }

  infix fun List<Pair<String, List<ProjectFindingReport>>>.shouldBe(
    expected: List<Pair<String, List<ProjectFindingReport>>>
  ) {
    sorted().trimmedShouldBe(expected.sorted(), RunnerTest::class)
  }
}
