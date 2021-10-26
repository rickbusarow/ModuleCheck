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

import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.core.MustBeApiFinding
import modulecheck.core.context.MustBeApi
import modulecheck.parsing.McProject

class MustBeApiRule(
  override val settings: ModuleCheckSettings
) : ModuleCheckRule<MustBeApiFinding>() {

  override val id = "MustBeApi"
  override val description = "Finds project dependencies which are exposed by the module " +
    "as part of its public ABI, but are only added as runtimeOnly, compileOnly, or implementation"

  override fun check(project: McProject): List<MustBeApiFinding> {
    return project[MustBeApi]
      .map {
        MustBeApiFinding(
          dependentPath = project.path,
          buildFile = project.buildFile,
          dependencyProject = it.configuredProjectDependency.project,
          configurationName = it.configuredProjectDependency.configurationName,
          source = it.source
        )
      }
    // .distinctBy { it.dependencyProject }
  }
}
