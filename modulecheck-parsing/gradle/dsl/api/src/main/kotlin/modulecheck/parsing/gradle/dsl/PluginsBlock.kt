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

package modulecheck.parsing.gradle.dsl

import modulecheck.finding.FindingName
import java.io.File

interface PluginsBlock :
  Block<PluginDeclaration>,
  HasSuppressedChildren<PluginDeclaration, FindingName> {
  fun getById(pluginId: String): PluginDeclaration?
}

interface PluginsBlockProvider {

  suspend fun get(): PluginsBlock?

  fun interface Factory {
    fun create(buildFile: File): PluginsBlockProvider
  }
}
