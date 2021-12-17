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

package modulecheck.project

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import modulecheck.parsing.gradle.AndroidGradleSettings
import modulecheck.parsing.gradle.AndroidGradleSettingsProvider
import modulecheck.parsing.gradle.DependenciesBlock
import modulecheck.parsing.gradle.DependenciesBlocksProvider
import modulecheck.parsing.gradle.PluginsBlock
import modulecheck.parsing.gradle.PluginsBlockProvider
import java.io.File

class BuildFileParser @AssistedInject constructor(
  dependenciesBlocksProviderFactory: DependenciesBlocksProvider.Factory,
  pluginsBlockProviderFactory: PluginsBlockProvider.Factory,
  androidGradleSettingsProviderFactory: AndroidGradleSettingsProvider.Factory,
  @Assisted
  private val buildFile: File
) {

  private val dependenciesBlocksProvider by lazy {
    dependenciesBlocksProviderFactory.create(buildFile)
  }
  private val pluginsBlockProvider by lazy {
    pluginsBlockProviderFactory.create(buildFile)
  }
  private val androidGradleSettingsProvider by lazy {
    androidGradleSettingsProviderFactory.create(buildFile)
  }

  fun pluginsBlock(): PluginsBlock? = pluginsBlockProvider.get()
  fun dependenciesBlocks(): List<DependenciesBlock> = dependenciesBlocksProvider.get()
  fun androidSettings(): AndroidGradleSettings = androidGradleSettingsProvider.get()

  @AssistedFactory
  interface Factory {
    fun create(buildFile: File): BuildFileParser
  }
}
