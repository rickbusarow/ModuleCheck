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

import modulecheck.parsing.gradle.dsl.DependencyDeclaration.ConfigurationNameTransform
import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.parsing.gradle.model.ProjectPath
import modulecheck.utils.letIf
import modulecheck.utils.prefixIfNot
import modulecheck.utils.remove
import modulecheck.utils.replaceDestructured

sealed interface DependencyDeclaration : BuildFileStatement {
  val configName: ConfigurationName

  fun interface ConfigurationNameTransform {
    suspend operator fun invoke(configurationName: ConfigurationName): String
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
  val projectPath: ProjectPath,
  val projectAccessor: ProjectAccessor,
  override val configName: ConfigurationName,
  override val declarationText: String,
  override val statementWithSurroundingText: String,
  override val suppressed: List<String> = emptyList(),
  val configurationNameTransform: ConfigurationNameTransform
) : DependencyDeclaration {

  suspend fun copy(
    newConfigName: ConfigurationName = configName,
    newModulePath: ProjectPath = projectPath,
    testFixtures: Boolean
  ): ModuleDependencyDeclaration {

    val newConfigText = configurationNameTransform(newConfigName)

    // Figure out if the old configuration is used as a string extension, like:
    // `"implementation"(...)`  instead of `implementation(...)`
    val quotedOldConfig = "\"${configName.value}\""
    val configIsInQuotes = declarationText.startsWith(quotedOldConfig)

    /*
    If the old config is a string extension, then we need to perform a String.replace(...) on
    the full string including the quotes, instead of just the configuration name. Otherwise, we
    can wind up with a precompiled config name (api, implementation, etc.) inside quotes.

    This isn't a very likely scenario if the SourceSet/Configuration hierarchies are working
    properly, but it's possible.  One scenario would be if the build file simply has an `"api"(...)`
    somewhere -- perhaps automatically added by the IDE's intention.  This might be the only string
    extension in the whole project, but without this check, autocorrect would use string extensions
    whenever that `"api"(...)` dependency is the source.
     */
    val configToReplace = if (configIsInQuotes) {
      quotedOldConfig
    } else {
      configName.value
    }

    val newModule = newModulePath != projectPath
    val precedingWhitespace = "^\\s*".toRegex()
      .find(statementWithSurroundingText)?.value ?: ""

    val newDeclaration = declarationText
      .letIf(newModule) {
        // strip out any config block
        it.remove(""" *\{[\s\S]*}""".toRegex())
      }
      .addOrRemoveTestFixtures(testFixtures)
      .replaceFirst(configToReplace, newConfigText)
      .replaceFirst(projectPath.value, newModulePath.value)
      .maybeFixExtraQuotes()

    val newStatement = when {
      newModule -> newDeclaration.prefixIfNot(precedingWhitespace)
      else -> statementWithSurroundingText.replaceFirst(declarationText, newDeclaration)
    }

    return copy(
      projectPath = newModulePath,
      configName = newConfigName,
      declarationText = newDeclaration,
      statementWithSurroundingText = newStatement
    )
  }

  /** replace any doubled up quotes with singles, like `""internalApi""` -> `"internalApi"` */
  private fun String.maybeFixExtraQuotes(): String {
    return replaceDestructured("\"\"([^\"]*)\"\"".toRegex()) { group1 ->
      "\"$group1\""
    }
  }

  private fun String.addOrRemoveTestFixtures(
    testFixtures: Boolean
  ): String {

    val escapedProjectAccessor = Regex.escape(projectAccessor.statementText)
    val regex = "testFixtures\\s*\\(\\s*$escapedProjectAccessor\\s*\\)".toRegex()

    return when {
      testFixtures && regex.containsMatchIn(this) -> this
      testFixtures -> "testFixtures($this)"
      else -> replace(regex, projectAccessor.statementText)
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ModuleDependencyDeclaration

    if (projectPath != other.projectPath) return false
    if (projectAccessor != other.projectAccessor) return false
    if (configName != other.configName) return false
    if (declarationText != other.declarationText) return false
    if (statementWithSurroundingText != other.statementWithSurroundingText) return false
    if (suppressed != other.suppressed) return false

    return true
  }

  override fun hashCode(): Int {
    var result = projectPath.hashCode()
    result = 31 * result + projectAccessor.hashCode()
    result = 31 * result + configName.hashCode()
    result = 31 * result + declarationText.hashCode()
    result = 31 * result + statementWithSurroundingText.hashCode()
    result = 31 * result + suppressed.hashCode()
    return result
  }

  override fun toString(): String {
    return """ModuleDependencyDeclaration(
      |  projectPath=$projectPath,
      |  projectAccessor=$projectAccessor,
      |  configName=$configName,
      |  declarationText='$declarationText',
      |  statementWithSurroundingText='$statementWithSurroundingText',
      |  suppressed=$suppressed
      |)
    """.trimMargin()
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

  override fun toString(): String {
    return """ExternalDependencyDeclaration(
      |  configName=$configName,
      |  declarationText='$declarationText',
      |  statementWithSurroundingText='$statementWithSurroundingText',
      |  suppressed=$suppressed,
      |  group=$group,
      |  moduleName=$moduleName,
      |  version=$version
      |)
    """.trimMargin()
  }
}
