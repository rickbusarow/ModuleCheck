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

package modulecheck.rule

import modulecheck.config.ModuleCheckSettings
import modulecheck.finding.Finding
import modulecheck.project.McProject

interface FindingFactory<T : Finding> {

  suspend fun evaluateFixable(projects: List<McProject>): List<T>
  suspend fun evaluateSorts(projects: List<McProject>): List<T>
  suspend fun evaluateReports(projects: List<McProject>): List<T>
}

fun interface RuleFilter {
  fun shouldEvaluate(rule: ModuleCheckRule<*>, settings: ModuleCheckSettings): Boolean

  companion object {
    val DEFAULT: RuleFilter = RuleFilter { rule, settings ->
      rule.shouldApply(settings)
    }
  }
}
