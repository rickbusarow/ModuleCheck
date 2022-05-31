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

import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.parsing.gradle.model.MavenCoordinates
import modulecheck.parsing.gradle.model.ProjectPath

fun UnknownDependencyDeclaration(
  argument: String,
  configName: ConfigurationName,
  declarationText: String,
  statementWithSurroundingText: String,
  suppressed: List<String> = emptyList()
): UnknownDependencyDeclaration = UnknownDependencyDeclaration(
  argument = argument,
  configName = configName,
  declarationText = declarationText,
  statementWithSurroundingText = statementWithSurroundingText,
  suppressed = suppressed,
  configurationNameTransform = { it.value }
)

@Suppress("LongParameterList")
fun ModuleDependencyDeclaration(
  projectPath: ProjectPath,
  projectAccessor: String,
  configName: ConfigurationName,
  declarationText: String,
  statementWithSurroundingText: String,
  suppressed: List<String> = emptyList()
): ModuleDependencyDeclaration = ModuleDependencyDeclaration(
  projectPath = projectPath,
  projectAccessor = ProjectAccessor.from(projectAccessor, projectPath),
  configName = configName,
  declarationText = declarationText,
  statementWithSurroundingText = statementWithSurroundingText,
  suppressed = suppressed
) { it.value }

@Suppress("LongParameterList")
fun ExternalDependencyDeclaration(
  configName: ConfigurationName,
  declarationText: String,
  statementWithSurroundingText: String,
  suppressed: List<String> = emptyList(),
  group: String?,
  moduleName: String,
  version: String?,
  coordinates: MavenCoordinates = MavenCoordinates(group, moduleName, version)
): ExternalDependencyDeclaration = ExternalDependencyDeclaration(
  configName = configName,
  declarationText = declarationText,
  statementWithSurroundingText = statementWithSurroundingText,
  suppressed = suppressed,
  configurationNameTransform = { it.value },
  group = group,
  moduleName = moduleName,
  version = version,
  coordinates = coordinates
)
