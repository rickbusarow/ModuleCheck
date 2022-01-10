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

import modulecheck.api.context.resSourceFiles
import modulecheck.api.rule.ModuleCheckRule
import modulecheck.api.settings.ChecksSettings
import modulecheck.core.rule.android.UnusedResourcesGenerationFinding
import modulecheck.project.AndroidMcProject
import modulecheck.project.McProject

class DisableAndroidResourcesRule : ModuleCheckRule<UnusedResourcesGenerationFinding> {

  override val id = "DisableAndroidResources"
  override val description =
    "Finds modules which have android resources R file generation enabled, " +
      "but don't actually use any resources from the module"

  @Suppress("ReturnCount")
  override suspend fun check(project: McProject): List<UnusedResourcesGenerationFinding> {
    val androidProject = project as? AndroidMcProject ?: return emptyList()

    @Suppress("UnstableApiUsage")
    if (!androidProject.androidResourcesEnabled) return emptyList()

    val noResources = androidProject.resSourceFiles().all().isEmpty()

    return if (noResources) {
      listOf(
        UnusedResourcesGenerationFinding(
          dependentProject = project, dependentPath = project.path, buildFile = project.buildFile
        )
      )
    } else {
      emptyList()
    }
  }

  override fun shouldApply(checksSettings: ChecksSettings): Boolean {
    return checksSettings.disableAndroidResources
  }
}
