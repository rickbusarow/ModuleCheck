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

import modulecheck.api.settings.ChecksSettings
import modulecheck.core.MustBeApiFinding
import modulecheck.core.context.MustBeApi
import modulecheck.project.McProject

class MustBeApiRule : DocumentedRule<MustBeApiFinding>() {

  override val id = "MustBeApi"
  override val description = "Finds project dependencies which are exposed by the module " +
    "as part of its public ABI, but are only added as runtimeOnly, compileOnly, or implementation"

  override val documentationPath: String = "must_be_api"

  override suspend fun check(project: McProject): List<MustBeApiFinding> {
    return project.get(MustBeApi)
      .map {
        val oldConfig = it.configuredProjectDependency.configurationName
        MustBeApiFinding(
          dependentProject = project,
          newDependency = it.configuredProjectDependency
            .copy(configurationName = oldConfig.apiVariant()),
          oldDependency = it.configuredProjectDependency,
          configurationName = oldConfig,
          source = it.source
        )
      }
  }

  override fun shouldApply(checksSettings: ChecksSettings): Boolean {
    return checksSettings.mustBeApi
  }
}
