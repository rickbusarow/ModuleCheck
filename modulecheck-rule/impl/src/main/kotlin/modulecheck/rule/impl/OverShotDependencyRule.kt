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
import modulecheck.core.context.overshotDependencies
import modulecheck.finding.OverShotDependencyFinding
import modulecheck.project.McProject
import javax.inject.Inject

class OverShotDependencyRule @Inject constructor(
  val settings: ModuleCheckSettings
) : DocumentedRule<OverShotDependencyFinding>() {

  override val name = OverShotDependencyFinding.NAME
  override val description = "Finds project dependencies which aren't used by the declaring" +
    " configuration, but are used by a dependent configuration."

  override suspend fun check(project: McProject): List<OverShotDependencyFinding> {
    return project.overshotDependencies()
      .all()
      .filterNot { it.newDependency.identifier.name in settings.ignoreUnusedFinding }
      .sortedByDescending { it.newDependency.configurationName }
      .map { it.toFinding() }
  }

  override fun shouldApply(settings: ModuleCheckSettings): Boolean {
    return settings.checks.overShotDependency
  }
}
