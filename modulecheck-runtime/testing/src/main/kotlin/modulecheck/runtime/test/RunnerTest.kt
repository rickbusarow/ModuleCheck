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
import modulecheck.project.ProjectProvider
import modulecheck.project.test.ProjectTest
import modulecheck.project.toTypeSafeProjectPathResolver
import modulecheck.reporting.checkstyle.CheckstyleReporter
import modulecheck.reporting.console.ReportFactory
import modulecheck.reporting.graphviz.GraphvizFactory
import modulecheck.reporting.graphviz.GraphvizFileWriter
import modulecheck.reporting.logging.McLogger
import modulecheck.reporting.logging.test.ReportingLogger
import modulecheck.reporting.sarif.SarifReportFactory
import modulecheck.rule.FindingFactory
import modulecheck.rule.ModuleCheckRule
import modulecheck.rule.RuleFilter
import modulecheck.rule.impl.FindingFactoryImpl
import modulecheck.rule.impl.RealFindingResultFactory
import modulecheck.rule.test.AllRulesComponent
import modulecheck.runtime.ModuleCheckRunner
import modulecheck.testing.trimmedShouldBe
import modulecheck.utils.mapToSet

@Suppress("UnnecessaryAbstractClass")
abstract class RunnerTest : ProjectTest() {

  open val settings: ModuleCheckSettings by resets { TestSettings() }
  open val logger: ReportingLogger by resets { ReportingLogger() }

  open val ruleFilter: RuleFilter = RuleFilter.DEFAULT

  open val rules: List<ModuleCheckRule<*>> by resets {
    AllRulesComponent.create(settings, ruleFilter).allRules
  }
  open val findingFactory: FindingFactory<out Finding> by resets {
    FindingFactoryImpl(rules)
  }

  override val codeGeneratorBindings: List<CodeGeneratorBinding>
    get() = settings.additionalCodeGenerators
      .plus(defaultCodeGeneratorBindings())
      .plus(
        @Suppress("DEPRECATION")
        settings.additionalKaptMatchers.mapToSet { it.toCodeGeneratorBinding() }
      )

  @Suppress("LongParameterList")
  fun run(
    autoCorrect: Boolean = true,
    strictResolution: Boolean = false,
    findingFactory: FindingFactory<out Finding> = this.findingFactory,
    settings: ModuleCheckSettings = this.settings,
    logger: McLogger = this.logger,
    projectProvider: ProjectProvider = this.projectProvider,
    findingResultFactory: FindingResultFactory = RealFindingResultFactory(),
    reportFactory: ReportFactory = ReportFactory(),
    checkstyleReporter: CheckstyleReporter = CheckstyleReporter(),
    graphvizFileWriter: GraphvizFileWriter = GraphvizFileWriter(
      settings = settings,
      graphvizFactory = GraphvizFactory(projectProvider.toTypeSafeProjectPathResolver())
    ),
    dispatcherProvider: DispatcherProvider = DispatcherProvider(),
    rules: DaggerList<ModuleCheckRule<*>> = this.rules
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
      projectProvider = projectProvider,
      sarifReportFactory = SarifReportFactory(
        websiteUrl = { "https://rbusarow.github.io/ModuleCheck" },
        moduleCheckVersion = { "0.12.1-SNAPSHOT" }
      ) { testProjectDir },
      rules = rules
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
