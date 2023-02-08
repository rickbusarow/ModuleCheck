/*
 * Copyright (C) 2021-2023 Rick Busarow
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

import modulecheck.model.dependency.HasConfigurations
import modulecheck.model.dependency.HasDependencies
import modulecheck.parsing.gradle.model.PluginAware
import java.io.File

interface InvokesConfigurationNames :
  PluginAware,
  HasBuildFile,
  HasConfigurations,
  HasDependencyDeclarations

interface HasBuildFile {
  val buildFile: File

  val buildFileParser: BuildFileParser
}

interface HasDependencyDeclarations :
  HasBuildFile,
  HasDependencies,
  HasConfigurations,
  PluginAware

suspend fun HasDependencyDeclarations.getConfigurationInvocations(): Set<String> {
  return buildFileParser.dependenciesBlocks()
    .flatMap { it.settings }
    .mapNotNull { declaration ->

      val declarationText = declaration.declarationText.trim()

      declaration.configName.value
        .takeIf { declarationText.startsWith(it) }
        ?: "\"${declaration.configName.value}\""
          .takeIf { declarationText.startsWith(it) }
    }
    .toSet()
}
