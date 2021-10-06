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

package modulecheck.parsing

abstract class DependenciesBlock(var contentString: String) {

  protected val originalLines = contentString.lines().toMutableList()

  private val _allDeclarations = mutableListOf<DependencyDeclaration>()

  val allDeclarations: List<DependencyDeclaration>
    get() = _allDeclarations

  protected val allExternalDeclarations =
    mutableMapOf<MavenCoordinates, MutableList<ExternalDependencyDeclaration>>()

  protected val allModuleDeclarations =
    mutableMapOf<ConfiguredModule, MutableList<ModuleDependencyDeclaration>>()
  protected val allBlockStatements = mutableListOf<String>()

  fun addNonModuleStatement(
    configName: String,
    parsedString: String,
    coordinates: MavenCoordinates
  ) {
    val originalString = getOriginalString(parsedString)

    val declaration = ExternalDependencyDeclaration(
      configName = configName,
      declarationText = parsedString,
      statementWithSurroundingText = originalString,
      group = coordinates.group,
      moduleName = coordinates.moduleName,
      version = coordinates.version
    )
    _allDeclarations.add(declaration)
    allExternalDeclarations.getOrPut(coordinates) { mutableListOf() }
      .add(declaration)
  }

  fun addUnknownStatement(
    configName: String,
    parsedString: String
  ) {
    val originalString = getOriginalString(parsedString)

    val declaration = UnknownDependencyDeclaration(
      configName = configName,
      declarationText = parsedString,
      statementWithSurroundingText = originalString
    )
    _allDeclarations.add(declaration)
  }

  fun addModuleStatement(
    moduleRef: String,
    configName: String,
    parsedString: String
  ) {
    val cm = ConfiguredModule(configName = configName, moduleRef = moduleRef)

    val originalString = getOriginalString(parsedString)

    val declaration = ModuleDependencyDeclaration(
      moduleRef = moduleRef,
      configName = configName,
      declarationText = parsedString,
      statementWithSurroundingText = originalString
    )

    allModuleDeclarations.getOrPut(cm) { mutableListOf() }
      .add(declaration)

    _allDeclarations.add(declaration)
  }

  fun getOrEmpty(moduleRef: String, configName: String): List<ModuleDependencyDeclaration> {
    require(moduleRef.startsWith(":")) {
      "The `moduleRef` parameter should be the traditional Gradle path, starting with ':'.  " +
        "Do not use the camel-cased type-safe project accessor.  This argument was '$moduleRef'."
    }

    return allModuleDeclarations[ConfiguredModule(configName, moduleRef)]
      ?: allModuleDeclarations[ConfiguredModule(configName, moduleRef.typeSafeName())]
      ?: emptyList()
  }

  fun getOrEmpty(
    mavenCoordinates: MavenCoordinates,
    configName: String
  ): List<ExternalDependencyDeclaration> {
    return allExternalDeclarations[mavenCoordinates]
      ?.filter { it.configName == configName }
      .orEmpty()
  }

  protected abstract fun findOriginalStringIndex(parsedString: String): Int

  protected fun getOriginalString(parsedString: String): String {
    val originalStringIndex = findOriginalStringIndex(parsedString)

    val originalStringLines = List(originalStringIndex + 1) {
      originalLines.removeFirst()
    }

    return originalStringLines.joinToString("\n")
  }

  protected data class ConfiguredModule(val configName: String, val moduleRef: String)
}
