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

package modulecheck.parsing.gradle

import modulecheck.parsing.gradle.DependencyDeclaration.ConfigurationNameTransform
import modulecheck.parsing.gradle.ModuleRef.StringRef
import modulecheck.parsing.gradle.ModuleRef.TypeSafeRef
import modulecheck.utils.replaceDestructured

sealed interface DependencyDeclaration : Declaration {
  val configName: ConfigurationName

  fun interface ConfigurationNameTransform {
    operator fun invoke(configurationName: ConfigurationName): String
  }
}

data class UnknownDependencyDeclaration(
  val argument: String,
  override val configName: ConfigurationName,
  override val declarationText: String,
  override val statementWithSurroundingText: String,
  override val suppressed: List<String> = emptyList(),
  val configurationNameTransform: ConfigurationNameTransform
) : DependencyDeclaration {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as UnknownDependencyDeclaration

    if (argument != other.argument) return false
    if (configName != other.configName) return false
    if (declarationText != other.declarationText) return false
    if (statementWithSurroundingText != other.statementWithSurroundingText) return false
    if (suppressed != other.suppressed) return false

    return true
  }

  override fun hashCode(): Int {
    var result = argument.hashCode()
    result = 31 * result + configName.hashCode()
    result = 31 * result + declarationText.hashCode()
    result = 31 * result + statementWithSurroundingText.hashCode()
    result = 31 * result + suppressed.hashCode()
    return result
  }
}

data class ModuleDependencyDeclaration(
  val moduleRef: ModuleRef,
  val moduleAccess: String,
  override val configName: ConfigurationName,
  override val declarationText: String,
  override val statementWithSurroundingText: String,
  override val suppressed: List<String> = emptyList(),
  val configurationNameTransform: ConfigurationNameTransform
) : DependencyDeclaration {

  fun replace(
    configName: ConfigurationName = this.configName,
    modulePath: String = this.moduleRef.value,
    testFixtures: Boolean
  ): ModuleDependencyDeclaration {

    val newConfigText = configurationNameTransform(configName)

    val newDeclaration = declarationText.addOrRemoveTestFixtures(testFixtures)
      .replaceFirst(this.configName.value, newConfigText)
      .replaceFirst(moduleRef.value, modulePath)
      .maybeFixDoubleQuotes()

    val newModuleRef = if (modulePath.startsWith(':')) {
      StringRef(modulePath)
    } else {
      TypeSafeRef(modulePath)
    }

    val newStatement = statementWithSurroundingText.replaceFirst(declarationText, newDeclaration)

    return copy(
      moduleRef = newModuleRef,
      configName = configName,
      declarationText = newDeclaration,
      statementWithSurroundingText = newStatement
    )
  }

  /** replace any doubled up quotes with singles, like `""internalApi""` -> `"internalApi"` */
  private fun String.maybeFixDoubleQuotes(): String {
    return replaceDestructured("\"\"([^\"]*)\"\"".toRegex()) { group1 ->
      "\"$group1\""
    }
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

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ModuleDependencyDeclaration

    if (moduleRef != other.moduleRef) return false
    if (moduleAccess != other.moduleAccess) return false
    if (configName != other.configName) return false
    if (declarationText != other.declarationText) return false
    if (statementWithSurroundingText != other.statementWithSurroundingText) return false
    if (suppressed != other.suppressed) return false

    return true
  }

  override fun hashCode(): Int {
    var result = moduleRef.hashCode()
    result = 31 * result + moduleAccess.hashCode()
    result = 31 * result + configName.hashCode()
    result = 31 * result + declarationText.hashCode()
    result = 31 * result + statementWithSurroundingText.hashCode()
    result = 31 * result + suppressed.hashCode()
    return result
  }
}

data class ExternalDependencyDeclaration(
  override val configName: ConfigurationName,
  override val declarationText: String,
  override val statementWithSurroundingText: String,
  override val suppressed: List<String> = emptyList(),
  val configurationNameTransform: ConfigurationNameTransform,
  val group: String?,
  val moduleName: String?,
  val version: String?
) : DependencyDeclaration {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ExternalDependencyDeclaration

    if (configName != other.configName) return false
    if (declarationText != other.declarationText) return false
    if (statementWithSurroundingText != other.statementWithSurroundingText) return false
    if (suppressed != other.suppressed) return false
    if (group != other.group) return false
    if (moduleName != other.moduleName) return false
    if (version != other.version) return false

    return true
  }

  override fun hashCode(): Int {
    var result = configName.hashCode()
    result = 31 * result + declarationText.hashCode()
    result = 31 * result + statementWithSurroundingText.hashCode()
    result = 31 * result + suppressed.hashCode()
    result = 31 * result + (group?.hashCode() ?: 0)
    result = 31 * result + (moduleName?.hashCode() ?: 0)
    result = 31 * result + (version?.hashCode() ?: 0)
    return result
  }
}
