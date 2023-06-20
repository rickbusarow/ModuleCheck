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

import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.MavenCoordinates
import modulecheck.model.dependency.ProjectPath

/**
 * Creates an instance of [UnknownDependencyDeclaration] with the given arguments.
 *
 * @param argument The argument that is unknown.
 * @param configName The name of the configuration this dependency belongs to.
 * @param declarationText The text representation of this declaration.
 * @param statementWithSurroundingText The declaration statement along with its surrounding context.
 * @param suppressed A list of suppressed warnings, empty by default.
 * @return An instance of [UnknownDependencyDeclaration].
 */
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

/**
 * Creates an instance of [ModuleDependencyDeclaration] with the given arguments.
 *
 * @param projectPath The path to the project this dependency refers to.
 * @param projectAccessor The string used to access the project in the dependency declaration.
 * @param configName The name of the configuration this dependency belongs to.
 * @param declarationText The text representation of this declaration.
 * @param statementWithSurroundingText The declaration statement along with its surrounding context.
 * @param suppressed A list of suppressed warnings, empty by default.
 * @return An instance of [ModuleDependencyDeclaration].
 */
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

/**
 * Creates an instance of [ExternalDependencyDeclaration] with the given arguments.
 *
 * @param configName The name of the configuration this dependency belongs to.
 * @param declarationText The text representation of this declaration.
 * @param statementWithSurroundingText The declaration statement along with its surrounding context.
 * @param suppressed A list of suppressed warnings, empty by default.
 * @param group The group of the external dependency.
 * @param moduleName The name of the module in the external dependency.
 * @param version The version of the external dependency.
 * @param coordinates The [MavenCoordinates] for the external
 *   dependency, computed from the group, module name and version.
 * @return An instance of [ExternalDependencyDeclaration].
 */
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
