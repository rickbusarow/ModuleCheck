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

package modulecheck.rule.impl

import com.squareup.anvil.annotations.ContributesTo
import dagger.Binds
import dagger.Module
import kotlinx.coroutines.flow.toList
import modulecheck.dagger.AppScope
import modulecheck.dagger.DaggerList
import modulecheck.finding.AddsDependency
import modulecheck.finding.Finding
import modulecheck.finding.ModifiesProjectDependency
import modulecheck.finding.OverShotDependencyFinding
import modulecheck.model.dependency.ConfiguredDependency
import modulecheck.project.McProject
import modulecheck.rule.FindingFactory
import modulecheck.rule.ModuleCheckRule
import modulecheck.rule.ReportOnlyRule
import modulecheck.rule.SortRule
import modulecheck.utils.sortedWith
import modulecheck.utils.trace.HasTraceTags
import modulecheck.utils.trace.traced
import javax.inject.Inject

@Module
@ContributesTo(AppScope::class)
interface FindingFactoryModule {
  // TODO maybe take another stab at removing the generic Finding type from FindingFactory
  @Binds
  fun FindingFactoryImpl.bindFindingFactory(): FindingFactory<out Finding>
}

/**
 * Sorts rules and applies the appropriate types for each function. Sorting is stable and
 * prioritizes modification rules so that they don't clobber each other.
 *
 * NB The incoming rules should already be filtered using [RuleFilter][modulecheck.rule.RuleFilter].
 * The filtering done within this class should only be done with regard to categorizing rules up by
 * fixable/sorts/reports categories.
 *
 * @since 0.12.0
 */
class FindingFactoryImpl @Inject constructor(
  private val rules: DaggerList<ModuleCheckRule<*>>
) : FindingFactory<Finding>, HasTraceTags {

  override val tags: Iterable<Any>
    get() = listOf(FindingFactoryImpl::class)

  override suspend fun evaluateFixable(projects: List<McProject>): List<Finding> {
    return evaluate(projects) { it !is SortRule && it !is ReportOnlyRule }
      .asSequence()
      // Use a stable but arbitrary sort before filtering out duplicates.  This makes it so that
      // if there are different finding types trying to modify the same dependency, the same one
      // will be chosen each time.
      .sortedBy { it.findingName.id }
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
            val newAdd = adding.add(finding.dependentProject to finding.newDependency)
            val newRemove = removing.add(finding.dependentProject to finding.oldDependency)
            if (newAdd || newRemove) {
              output.add(finding)
            }
          }

          is AddsDependency -> {
            if (adding.add(finding.dependentProject to finding.newDependency)) {
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
    return ProjectQueue(projects)
      .process { project ->
        rules.filter { rule -> predicate(rule) }
          .flatMap { rule ->
            traced(project, rule) { rule.check(project) }
          }
      }
      .toList()
      .flatten()
  }
}
