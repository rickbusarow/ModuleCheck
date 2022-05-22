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

import modulecheck.config.ModuleCheckSettings
import modulecheck.core.context.UnusedDependencies
import modulecheck.finding.FindingName
import modulecheck.finding.UnusedDependencyFinding
import modulecheck.project.McProject
import javax.inject.Inject

class UnusedDependencyRule @Inject constructor(
  private val settings: ModuleCheckSettings
) : DocumentedRule<UnusedDependencyFinding>() {

  override val name = FindingName("unused-dependency")
  override val description = "Finds project dependencies which aren't used in the declaring module"

  override suspend fun check(project: McProject): List<UnusedDependencyFinding> {
    return project.get(UnusedDependencies)
      .all()
      .filterNot { it.dependency.path.value in settings.ignoreUnusedFinding }
      .distinctBy { it.dependency }
      .map { it.toFinding(name) }
  }

  override fun shouldApply(settings: ModuleCheckSettings): Boolean {
    return settings.checks.unusedDependency
  }
}
