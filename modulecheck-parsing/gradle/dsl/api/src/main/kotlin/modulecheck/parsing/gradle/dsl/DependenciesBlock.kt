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
import modulecheck.parsing.gradle.dsl.DependenciesBlock.ConfiguredModule
import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.parsing.gradle.model.MavenCoordinates
import modulecheck.parsing.gradle.model.ProjectPath

interface DependenciesBlock :
  Block<DependencyDeclaration>,
  HasSuppressedChildren<ConfiguredModule, FindingName> {

  fun getOrEmpty(
    moduleRef: String,
    configName: ConfigurationName,
    testFixtures: Boolean
  ): List<ModuleDependencyDeclaration>

  fun getOrEmpty(
    moduleRef: ProjectPath,
    configName: ConfigurationName,
    testFixtures: Boolean
  ): List<ModuleDependencyDeclaration>

  fun getOrEmpty(
    mavenCoordinates: MavenCoordinates,
    configName: ConfigurationName
  ): List<ExternalDependencyDeclaration>

  data class ConfiguredModule(
    val configName: ConfigurationName,
    val projectPath: ProjectPath,
    val testFixtures: Boolean
  )
}

interface DependenciesBlocksProvider {

  suspend fun get(): List<DependenciesBlock>

  fun interface Factory {
    fun create(invokesConfigurationNames: InvokesConfigurationNames): DependenciesBlocksProvider
  }
}
