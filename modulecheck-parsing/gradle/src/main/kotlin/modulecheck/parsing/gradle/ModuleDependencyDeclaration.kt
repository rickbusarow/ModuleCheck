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

package modulecheck.parsing.gradle

import modulecheck.parsing.gradle.ModuleRef.StringRef
import modulecheck.parsing.gradle.ModuleRef.TypeSafeRef

sealed interface DependencyDeclaration : Declaration {
  val configName: ConfigurationName
}

data class UnknownDependencyDeclaration(
  val argument: String,
  override val configName: ConfigurationName,
  override val declarationText: String,
  override val statementWithSurroundingText: String,
  override val suppressed: List<String> = emptyList()
) : DependencyDeclaration

data class ModuleDependencyDeclaration(
  val moduleRef: ModuleRef,
  val moduleAccess: String,
  override val configName: ConfigurationName,
  override val declarationText: String,
  override val statementWithSurroundingText: String,
  override val suppressed: List<String> = emptyList()
) : DependencyDeclaration {

  fun replace(
    configName: ConfigurationName = this.configName,
    modulePath: String = this.moduleRef.value,
    testFixtures: Boolean
  ): ModuleDependencyDeclaration {

    val newDeclaration = declarationText.addOrRemoveTestFixtures(testFixtures)
      .replaceFirst(this.configName.value, configName.value)
      .replaceFirst(moduleRef.value, modulePath)

    val newModuleRef = if (modulePath.startsWith(':')) {
      StringRef(modulePath)
    } else {
      TypeSafeRef(modulePath)
    }

    val newStatement = statementWithSurroundingText.replaceFirst(declarationText, newDeclaration)

    return ModuleDependencyDeclaration(
      moduleRef = newModuleRef,
      moduleAccess = moduleAccess,
      configName = configName,
      declarationText = newDeclaration,
      statementWithSurroundingText = newStatement,
      suppressed = suppressed
    )
  }

  private fun String.addOrRemoveTestFixtures(
    testFixtures: Boolean
  ): String {

    val escapedModuleAccess = Regex.escape(moduleAccess)
    val regex = "testFixtures\\s*\\(\\s*$escapedModuleAccess\\s*\\)".toRegex()

    return when {
      testFixtures && regex.containsMatchIn(this) -> this
      testFixtures -> "testFixtures($this)"
      else -> replace(regex, moduleAccess)
    }
  }
}

data class ExternalDependencyDeclaration(
  override val configName: ConfigurationName,
  override val declarationText: String,
  override val statementWithSurroundingText: String,
  override val suppressed: List<String> = emptyList(),
  val group: String?,
  val moduleName: String?,
  val version: String?
) : DependencyDeclaration
