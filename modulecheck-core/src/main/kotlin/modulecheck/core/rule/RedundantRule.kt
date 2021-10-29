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

import modulecheck.api.ModuleCheckRule
import modulecheck.api.settings.ChecksSettings
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.core.context.RedundantDependencies
import modulecheck.core.context.RedundantDependencyFinding
import modulecheck.parsing.McProject
import modulecheck.parsing.all

class RedundantRule(
  override val settings: ModuleCheckSettings
) : ModuleCheckRule<RedundantDependencyFinding> {

  override val id = "RedundantDependency"
  override val description =
    "Finds project dependencies which are declared as `api` in dependent " +
      "projects, but also declared in the current project unnecessarily"

  override fun check(project: McProject): List<RedundantDependencyFinding> {
    return project[RedundantDependencies]
      .all()
      .distinctBy { it.positionOrNull }
  }

  override fun shouldApply(checksSettings: ChecksSettings): Boolean {
    return checksSettings.redundantDependency
  }
}
