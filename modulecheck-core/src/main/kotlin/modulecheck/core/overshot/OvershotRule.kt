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

package modulecheck.core.overshot

import modulecheck.api.Project2
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.core.mcp
import modulecheck.core.rule.AbstractRule

class OvershotRule(
  override val settings: ModuleCheckSettings
) : AbstractRule<OvershotDependencyFinding>() {

  override val id = "OvershotDependency"

  override fun check(project: Project2): List<OvershotDependencyFinding> {
    return project.mcp().overshot
      .all()
      .distinctBy { it.dependencyIdentifier }
  }
}
