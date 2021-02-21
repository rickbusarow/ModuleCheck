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

package modulecheck.core.rule.android

import modulecheck.api.AndroidProject2
import modulecheck.api.Project2
import modulecheck.api.settings.ModuleCheckSettings
import modulecheck.core.rule.ModuleCheckRule
import modulecheck.psi.DslBlockVisitor
import net.swiftzer.semver.SemVer

internal val androidBlockParser = DslBlockVisitor("android")
internal val androidBlockRegex = "^android \\{".toRegex()

private val MINIMUM_ANDROID_RESOURCES_VERSION = SemVer(major = 4, minor = 1, patch = 0)

class DisableAndroidResourcesRule(
  override val settings: ModuleCheckSettings
) : ModuleCheckRule<UnusedResourcesGenerationFinding>() {

  override val id = "DisableAndroidResources"
  override val description =
    "Finds modules which have android resources R file generation enabled, " +
      "but don't actually use any resources from the module"

  @Suppress("ReturnCount")
  override fun check(project: Project2): List<UnusedResourcesGenerationFinding> {
    val androidProject = project as? AndroidProject2 ?: return emptyList()

    // grabs the AGP version of the client project - not this plugin
    val agpVersion = androidProject.agpVersion

    // minimum AGP version for this feature is 4.1.0, so don't bother checking below that
    if (agpVersion < MINIMUM_ANDROID_RESOURCES_VERSION) return emptyList()

    @Suppress("UnstableApiUsage")
    if (!androidProject.androidResourcesEnabled) return emptyList()

    val noResources = androidProject.resourceFiles.isEmpty()

    return if (noResources) {
      listOf(UnusedResourcesGenerationFinding(project.path, project.buildFile))
    } else {
      emptyList()
    }
  }
}
