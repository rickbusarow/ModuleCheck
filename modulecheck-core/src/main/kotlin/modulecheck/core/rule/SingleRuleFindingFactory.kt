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

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import modulecheck.api.finding.Finding
import modulecheck.api.finding.FindingFactory
import modulecheck.api.rule.ModuleCheckRule
import modulecheck.api.rule.ReportOnlyRule
import modulecheck.api.rule.SortRule
import modulecheck.project.McProject

class SingleRuleFindingFactory<T : Finding>(
  val rule: ModuleCheckRule<T>
) : FindingFactory<Finding> {

  override suspend fun evaluateFixable(projects: List<McProject>): List<T> {
    return if (rule !is SortRule && rule !is ReportOnlyRule) {
      evaluateRule(projects)
    } else emptyList()
  }

  override suspend fun evaluateSorts(projects: List<McProject>): List<T> {
    return if (rule is SortRule) {
      evaluateRule(projects)
    } else emptyList()
  }

  override suspend fun evaluateReports(projects: List<McProject>): List<T> {
    return if (rule is ReportOnlyRule) {
      evaluateRule(projects)
    } else emptyList()
  }

  private suspend fun evaluateRule(projects: List<McProject>): List<T> {
    return coroutineScope {
      projects
        .map { project -> async { rule.check(project) } }
        .awaitAll()
        .flatten()
    }
  }
}
