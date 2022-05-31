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

package modulecheck.model.dependency

import modulecheck.config.CodeGeneratorBinding
import modulecheck.config.MightHaveCodeGeneratorBinding
import modulecheck.parsing.gradle.model.ConfigurationName
import modulecheck.parsing.gradle.model.MavenCoordinates
import modulecheck.utils.lazy.unsafeLazy

sealed class ExternalDependency : ConfiguredDependency {
  abstract val group: String?
  abstract val moduleName: String
  abstract val version: String?

  val coords by unsafeLazy { MavenCoordinates(group, moduleName, version) }
  override val identifier by unsafeLazy { "${group ?: ""}:$moduleName" }
  val nameWithVersion by unsafeLazy { "${group ?: ""}:$moduleName:${version ?: ""}" }

  class ExternalRuntimeDependency(
    override val configurationName: ConfigurationName,
    override val group: String?,
    override val moduleName: String,
    override val version: String?,
    override val isTestFixture: Boolean
  ) : ExternalDependency()

  class ExternalCodeGeneratorDependency(
    override val configurationName: ConfigurationName,
    override val group: String?,
    override val moduleName: String,
    override val version: String?,
    override val isTestFixture: Boolean,
    override val codeGeneratorBindingOrNull: CodeGeneratorBinding?
  ) : ExternalDependency(), MightHaveCodeGeneratorBinding

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ExternalDependency

    if (configurationName != other.configurationName) return false
    if (group != other.group) return false
    if (moduleName != other.moduleName) return false
    if (version != other.version) return false

    return true
  }

  override fun hashCode(): Int {
    var result = javaClass.hashCode()
    result = 31 * result + configurationName.hashCode()
    result = 31 * result + group.hashCode()
    result = 31 * result + moduleName.hashCode()
    result = 31 * result + version.hashCode()
    return result
  }

  /** Let's pretend this is a data class. */
  fun copy(
    configurationName: ConfigurationName = this.configurationName,
    group: String? = this.group,
    moduleName: String = this.moduleName,
    version: String? = this.version,
    isTestFixture: Boolean = this.isTestFixture
  ): ExternalDependency {
    return when (this) {
      is ExternalRuntimeDependency -> ExternalRuntimeDependency(
        configurationName = configurationName,
        group = group,
        moduleName = moduleName,
        version = version,
        isTestFixture = isTestFixture
      )

      is ExternalCodeGeneratorDependency -> ExternalCodeGeneratorDependency(
        configurationName = configurationName,
        group = group,
        moduleName = moduleName,
        version = version,
        isTestFixture = isTestFixture,
        codeGeneratorBindingOrNull = codeGeneratorBindingOrNull
      )
    }
  }

  /**
   * Creates an [ExternalDependency] for given arguments, a `List<CodeGeneratorBinding>` to look up
   * a [CodeGeneratorBinding] in the event that the project dependency in question is an annotation
   * processor.
   */
  fun interface Factory {

    /** @return the [ProjectDependency] for this dependency declaration */
    fun create(
      configurationName: ConfigurationName,
      group: String?,
      moduleName: String,
      version: String?
    ): ExternalDependency
  }
}
