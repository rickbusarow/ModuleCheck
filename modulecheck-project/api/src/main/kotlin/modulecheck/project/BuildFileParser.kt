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

package modulecheck.project

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import modulecheck.parsing.gradle.AndroidGradleSettings
import modulecheck.parsing.gradle.AndroidGradleSettingsProvider
import modulecheck.parsing.gradle.DependenciesBlock
import modulecheck.parsing.gradle.DependenciesBlocksProvider
import modulecheck.parsing.gradle.InvokesConfigurationNames
import modulecheck.parsing.gradle.PluginsBlock
import modulecheck.parsing.gradle.PluginsBlockProvider

class BuildFileParser @AssistedInject constructor(
  dependenciesBlocksProviderFactory: DependenciesBlocksProvider.Factory,
  pluginsBlockProviderFactory: PluginsBlockProvider.Factory,
  androidGradleSettingsProviderFactory: AndroidGradleSettingsProvider.Factory,
  @Assisted
  private val invokesConfigurationNames: InvokesConfigurationNames
) {

  private val dependenciesBlocksProvider by lazy {
    dependenciesBlocksProviderFactory.create(invokesConfigurationNames)
  }
  private val pluginsBlockProvider by lazy {
    pluginsBlockProviderFactory.create(invokesConfigurationNames.buildFile)
  }
  private val androidGradleSettingsProvider by lazy {
    androidGradleSettingsProviderFactory.create(invokesConfigurationNames.buildFile)
  }

  private val lock = Mutex(locked = false)

  suspend fun pluginsBlock(): PluginsBlock? = lock.withLock { pluginsBlockProvider.get() }
  suspend fun dependenciesBlocks(): List<DependenciesBlock> =
    lock.withLock { dependenciesBlocksProvider.get() }

  suspend fun androidSettings(): AndroidGradleSettings =
    lock.withLock { androidGradleSettingsProvider.get() }

  @AssistedFactory
  fun interface Factory {
    fun create(invokesConfigurationNames: InvokesConfigurationNames): BuildFileParser
  }
}
