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

package modulecheck.core.rule

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import modulecheck.api.Finding
import modulecheck.api.FindingFactory
import modulecheck.api.ModuleCheckRule
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.parsing.McProject

class SingleRuleFindingFactory<T : Finding>(
  val rule: ModuleCheckRule<T>
) : FindingFactory<Finding> {

  override suspend fun evaluate(projects: List<McProject>): List<T> {
    return coroutineScope {
      projects
        .map { project -> async { rule.check(project) } }
        .awaitAll()
        .flatten()
    }
  }
}

class MultiRuleFindingFactory(
  private val settings: ModuleCheckSettings,
  private val rules: List<ModuleCheckRule<out Finding>>
) : FindingFactory<Finding> {

  override suspend fun evaluate(projects: List<McProject>): List<Finding> {
    return coroutineScope {
      projects.flatMap { project ->
        rules
          .filter { it.shouldApply(settings.checks) }
          .map { rule -> async { rule.check(project) } }
      }
        .awaitAll()
        .flatten()
    }
  }
}
