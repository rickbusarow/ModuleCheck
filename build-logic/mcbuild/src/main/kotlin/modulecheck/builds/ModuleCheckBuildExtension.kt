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

package modulecheck.builds

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.PluginManager
import javax.inject.Inject

@Suppress("MemberVisibilityCanBePrivate", "UnnecessaryAbstractClass")
abstract class ModuleCheckBuildExtension
@Inject constructor(
  objects: ObjectFactory,
  private val pluginManager: PluginManager,
  private val project: Project
) : ArtifactIdExtension,
  BuildPropertiesExtension,
  DiExtension {

  override var artifactId: String? by objects.optionalProperty {
    project.configurePublishing(it)
  }

  override fun anvil() {
    project.applyAnvil()
  }

  override fun dagger() {
    project.applyDagger()
  }

  fun ksp() {
    pluginManager.apply("com.google.devtools.ksp")
  }
}
