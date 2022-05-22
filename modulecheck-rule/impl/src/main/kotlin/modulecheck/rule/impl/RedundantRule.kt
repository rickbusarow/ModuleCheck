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
import modulecheck.core.context.RedundantDependencies
import modulecheck.finding.FindingName
import modulecheck.finding.RedundantDependencyFinding
import modulecheck.project.McProject
import javax.inject.Inject

class RedundantRule @Inject constructor() : DocumentedRule<RedundantDependencyFinding>() {

  override val name = FindingName("redundant-dependency")
  override val description =
    "Finds project dependencies which are declared as `api` in dependent " +
      "projects, but also declared in the current project unnecessarily"

  override suspend fun check(project: McProject): List<RedundantDependencyFinding> {
    return project.get(RedundantDependencies)
      .all()
      .distinctBy { it.dependency }
      .map { it.toFinding(findingName = name) }
  }

  override fun shouldApply(settings: ModuleCheckSettings): Boolean {
    return settings.checks.redundantDependency
  }
}
