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

package modulecheck.model.dependency

import modulecheck.utils.lazy.unsafeLazy

/**
 * Represents an external dependency, such as a JAR file or a Maven artifact.
 *
 * @since 0.12.0
 */
sealed class ExternalDependency :
  ConfiguredDependency,
  HasMavenCoordinates,
  HasMavenCoordinatesElements {

  /** The group ID of the dependency, like `org.jetbrains.kotlin` */
  override val group: String? get() = mavenCoordinates.group
  override val moduleName: String get() = mavenCoordinates.moduleName
  override val version: String? get() = mavenCoordinates.version

  override val identifier: MavenCoordinates by unsafeLazy { mavenCoordinates }
  val nameWithVersion: String by unsafeLazy {
    "${group.orEmpty()}:$moduleName:${version.orEmpty()}"
  }
  val nameWithoutVersion: String by unsafeLazy { "${group.orEmpty()}:$moduleName" }

  class ExternalRuntimeDependency(
    override val configurationName: ConfigurationName,
    override val mavenCoordinates: MavenCoordinates,
    override val isTestFixture: Boolean
  ) : ExternalDependency() {
    constructor(
      configurationName: ConfigurationName,
      group: String?,
      moduleName: String,
      version: String?,
      isTestFixture: Boolean
    ) : this(
      configurationName = configurationName,
      mavenCoordinates = MavenCoordinates(
        group = group,
        moduleName = moduleName,
        version = version
      ),
      isTestFixture = isTestFixture
    )
  }

  class ExternalCodeGeneratorDependency(
    override val configurationName: ConfigurationName,
    override val mavenCoordinates: MavenCoordinates,
    override val isTestFixture: Boolean,
    override val codeGeneratorBindingOrNull: CodeGenerator?
  ) : ExternalDependency(), MightHaveCodeGeneratorBinding {

    constructor(
      configurationName: ConfigurationName,
      group: String?,
      moduleName: String,
      version: String?,
      isTestFixture: Boolean,
      codeGeneratorBindingOrNull: CodeGenerator?
    ) : this(
      configurationName = configurationName,
      mavenCoordinates = MavenCoordinates(
        group = group,
        moduleName = moduleName,
        version = version
      ),
      isTestFixture = isTestFixture,
      codeGeneratorBindingOrNull = codeGeneratorBindingOrNull
    )
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ExternalDependency

    if (configurationName != other.configurationName) return false
    if (mavenCoordinates != other.mavenCoordinates) return false

    return true
  }

  override fun hashCode(): Int {
    var result = javaClass.hashCode()
    result = 31 * result + configurationName.hashCode()
    result = 31 * result + mavenCoordinates.hashCode()
    return result
  }

  /**
   * Let's pretend this is a data class.
   *
   * @since 0.12.0
   */
  fun copy(
    configurationName: ConfigurationName = this.configurationName,
    group: String? = this.group,
    moduleName: String = this.moduleName,
    version: String? = this.version,
    isTestFixture: Boolean = this.isTestFixture
  ): ExternalDependency {

    val coordinates = if (
      group != this.group ||
      moduleName != this.moduleName ||
      version != this.version
    ) {
      MavenCoordinates(group = group, moduleName = moduleName, version = version)
    } else {
      mavenCoordinates
    }

    return copy(
      configurationName = configurationName,
      mavenCoordinates = coordinates,
      isTestFixture = isTestFixture
    )
  }

  /** Let's pretend this is a data class. */
  fun copy(
    configurationName: ConfigurationName = this.configurationName,
    mavenCoordinates: MavenCoordinates = this.mavenCoordinates,
    isTestFixture: Boolean = this.isTestFixture
  ): ExternalDependency {

    val coordinates = if (
      group != this.group ||
      moduleName != this.moduleName ||
      version != this.version
    ) {
      MavenCoordinates(group = group, moduleName = moduleName, version = version)
    } else {
      mavenCoordinates
    }

    return when (this) {
      is ExternalRuntimeDependency -> ExternalRuntimeDependency(
        configurationName = configurationName,
        mavenCoordinates = coordinates,
        isTestFixture = isTestFixture
      )

      is ExternalCodeGeneratorDependency -> ExternalCodeGeneratorDependency(
        configurationName = configurationName,
        mavenCoordinates = coordinates,
        isTestFixture = isTestFixture,
        codeGeneratorBindingOrNull = codeGeneratorBindingOrNull
      )
    }
  }

  override fun toString(): String {

    val declaration = if (isTestFixture) {
      "${configurationName.value}(testFixtures(\"${nameWithVersion}\"))"
    } else {
      "${configurationName.value}(\"${nameWithVersion}\")"
    }

    return "${javaClass.simpleName}( $declaration )"
  }

  /**
   * Creates an [ExternalDependency] for given arguments, a
   * `List<CodeGeneratorBinding>` to look up a [CodeGenerator] in the event
   * that the project dependency in question is an annotation processor.
   *
   * @since 0.12.0
   */
  fun interface Factory {

    /**
     * @return the [ProjectDependency] for this dependency declaration
     * @since 0.12.0
     */
    fun create(
      configurationName: ConfigurationName,
      group: String?,
      moduleName: String,
      version: String?,
      isTestFixture: Boolean
    ): ExternalDependency
  }
}
