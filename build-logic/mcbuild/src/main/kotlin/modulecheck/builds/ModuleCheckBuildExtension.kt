/*
 * Copyright (C) 2021-2024 Rick Busarow
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

import modulecheck.builds.matrix.VersionsMatrixExtension
import org.gradle.api.Project
import javax.inject.Inject

@Suppress("MemberVisibilityCanBePrivate", "UnnecessaryAbstractClass")
abstract class ModuleCheckBuildExtension @Inject constructor(
  private val target: Project
) : VersionsMatrixExtension(target),
  BuildPropertiesExtension,
  PublishingExtension,
  DiExtension {

  override fun anvil() {
    target.applyAnvil()
  }

  override fun dagger() {
    target.applyDagger()
  }

  fun ksp() {
    target.pluginManager.apply("com.google.devtools.ksp")
  }
}
