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

import modulecheck.api.rule.RuleName
import modulecheck.api.settings.ChecksSettings
import modulecheck.core.anvil.AnvilFactoryParser
import modulecheck.core.anvil.CouldUseAnvilFinding
import modulecheck.project.McProject

class AnvilFactoryRule : DocumentedRule<CouldUseAnvilFinding>() {

  override val name = RuleName("use-anvil-factory-generation")
  override val description = "Finds modules which could use Anvil's factory generation " +
    "instead of Dagger's"

  override suspend fun check(project: McProject): List<CouldUseAnvilFinding> {
    return AnvilFactoryParser.parse(name, project)
  }

  override fun shouldApply(checksSettings: ChecksSettings): Boolean {
    return checksSettings.anvilFactoryGeneration
  }
}
