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

import com.squareup.anvil.plugin.AnvilExtension
import modulecheck.api.Project2
import modulecheck.api.context.ProjectContext
import net.swiftzer.semver.SemVer
import org.gradle.kotlin.dsl.findByType

data class AnvilGradlePlugin(
  val version: SemVer,
  val generateDaggerFactories: Boolean
) : ProjectContext.Element {

  override val key: ProjectContext.Key<AnvilGradlePlugin>
    get() = Key

  companion object Key : ProjectContext.Key<AnvilGradlePlugin> {
    override operator fun invoke(project: Project2): AnvilGradlePlugin {
      if (project !is Project2Gradle) {
        return AnvilGradlePlugin(SemVer(0, 0, 0), false)
      }

      val version = project
        .gradleProject
        .configurations
        .findByName("kotlinCompilerPluginClasspath")
        ?.dependencies
        ?.find { it.group == "com.squareup.anvil" }
        ?.version
        ?.let { versionString -> SemVer.parse(versionString) }
        ?: SemVer(0, 0, 0)

      val enabled = project
        .gradleProject
        .extensions
        .findByType<AnvilExtension>()
        ?.generateDaggerFactories == true

      return AnvilGradlePlugin(version, enabled)
    }
  }
}

val ProjectContext.anvilGradlePlugin: AnvilGradlePlugin get() = get(AnvilGradlePlugin)
