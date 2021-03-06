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

package modulecheck.api.settings

@Suppress("UnstableApiUsage")
class ChecksExtension : ChecksSettings {
  override var overshotDependency: Boolean = true
  override var redundantDependency: Boolean = false
  override var unusedDependency: Boolean = true
  override var mustBeApi: Boolean = true
  override var inheritedImplementation: Boolean = true
  override var sortDependencies: Boolean = false
  override var sortPlugins: Boolean = false
  override var unusedKapt: Boolean = true
  override var anvilFactoryGeneration: Boolean = true
  override var disableAndroidResources: Boolean = false
  override var disableViewBinding: Boolean = false
}

@Suppress("UnstableApiUsage")
class SortExtension : SortSettings {
  override var pluginComparators = listOf(
    """id\("com\.android.*"\)""",
    """id\("android-.*"\)""",
    """id\("java-library"\)""",
    """kotlin\("jvm"\)""",
    """android.*""",
    """javaLibrary.*""",
    """kotlin.*""",
    """id.*"""
  )
  override var dependencyComparators = listOf(
    """.*""",
    """kapt.*"""
  )
}
