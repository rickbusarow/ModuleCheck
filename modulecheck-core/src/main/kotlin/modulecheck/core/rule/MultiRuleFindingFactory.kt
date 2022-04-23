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

package modulecheck.core.rule

import kotlinx.coroutines.flow.toList
import modulecheck.api.context.depths
import modulecheck.api.finding.AddsDependency
import modulecheck.api.finding.Finding
import modulecheck.api.finding.FindingFactory
import modulecheck.api.finding.ModifiesProjectDependency
import modulecheck.api.rule.ModuleCheckRule
import modulecheck.api.rule.ReportOnlyRule
import modulecheck.api.rule.SortRule
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.core.OverShotDependencyFinding
import modulecheck.parsing.gradle.SourceSetName
import modulecheck.project.ConfiguredDependency
import modulecheck.project.McProject
import modulecheck.utils.mapAsync
import modulecheck.utils.sortedWith

class MultiRuleFindingFactory(
  private val settings: ModuleCheckSettings,
  private val rules: List<ModuleCheckRule<out Finding>>
) : FindingFactory<Finding> {

  override suspend fun evaluateFixable(projects: List<McProject>): List<Finding> {
    return evaluate(projects) { it !is SortRule && it !is ReportOnlyRule }
      .asSequence()
      // Use a stable but arbitrary sort before filtering out duplicates.  This makes it so that
      // if there are different finding types trying to modify the same dependency, the same one
      // will be chosen each time.
      .sortedBy { it.findingName }
      .filterDuplicateAdds()
      .toList()
  }

  private fun Sequence<Finding>.filterDuplicateAdds(): List<Finding> {

    val adding = mutableSetOf<Pair<McProject, ConfiguredDependency>>()
    val removing = mutableSetOf<Pair<McProject, ConfiguredDependency>>()

    val output = mutableListOf<Finding>()

    sortedWith(
      { it !is ModifiesProjectDependency },
      { it !is OverShotDependencyFinding }
    )
      .forEach { finding ->
        when (finding) {

          is ModifiesProjectDependency -> {
            val newAdd = adding.add(finding.subjectProject to finding.newDependency)
            val newRemove = removing.add(finding.subjectProject to finding.oldDependency)
            if (newAdd || newRemove) {
              output.add(finding)
            }
          }
          is AddsDependency -> {
            if (adding.add(finding.subjectProject to finding.newDependency)) {
              output.add(finding)
            }
          }
          else -> {
            output.add(finding)
          }
        }
      }
    return output
  }

  override suspend fun evaluateSorts(projects: List<McProject>): List<Finding> {
    return evaluate(projects) { it is SortRule }
  }

  override suspend fun evaluateReports(projects: List<McProject>): List<Finding> {
    return evaluate(projects) { it is ReportOnlyRule }
  }

  private suspend fun evaluate(
    projects: List<McProject>,
    predicate: (ModuleCheckRule<*>) -> Boolean
  ): List<Finding> {

    // Sort all projects by depth, so that zero-dependency modules are first and the most expensive
    // are last.  This is just a simple way of building up cached data, so that when the
    // heavy-weight modules are evaluated, they can pull declarations and other data from cache.
    val sortedProjects = projects.mapAsync { it.depths().get(SourceSetName.MAIN).depth to it }
      .toList()
      .sortedBy { it.first }
      .map { it.second }

    return rules.filter { predicate(it) && it.shouldApply(settings.checks) }
      .flatMap { rule ->
        sortedProjects.mapAsync { project ->
          rule.check(project)
        }.toList()
      }.flatten()
  }
}
