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

package modulecheck.gradle

import modulecheck.api.Finding
import modulecheck.core.rule.ModuleCheckRule
import modulecheck.gradle.task.ModuleCheckTask
import modulecheck.parsing.McProject
import org.gradle.api.tasks.Internal
import javax.inject.Inject

abstract class DynamicModuleCheckTask<T : Finding> @Inject constructor(
  @Internal
  val rule: ModuleCheckRule<T>
) : ModuleCheckTask<T>() {

  init {
    description = rule.description
  }

  override fun List<McProject>.evaluate(): List<T> {
    return flatMap { project ->
      rule.check(project)
    }
  }
}
