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

package modulecheck.benchmark

import modulecheck.config.CodeGeneratorBinding
import modulecheck.config.internal.defaultCodeGeneratorBindings
import modulecheck.finding.internal.addDependency
import modulecheck.finding.internal.addStatement
import modulecheck.finding.internal.prependStatement
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.ConfiguredDependency
import modulecheck.model.dependency.ExternalDependency
import modulecheck.model.dependency.ProjectDependency
import modulecheck.model.dependency.TypeSafeProjectPathResolver
import modulecheck.model.dependency.impl.RealConfiguredProjectDependencyFactory
import modulecheck.parsing.gradle.dsl.DependencyDeclaration
import modulecheck.parsing.gradle.dsl.asDeclaration
import modulecheck.parsing.kotlin.compiler.impl.SafeAnalysisResultAccess
import modulecheck.parsing.kotlin.compiler.impl.SafeAnalysisResultAccessImpl
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.project.generation.McProjectBuilder
import modulecheck.project.generation.ProjectCollector
import java.io.File

class BenchmarkBuilder(
  val numModules: Int,
  override val root: File,
  override val projectCache: ProjectCache = ProjectCache(),
  override val safeAnalysisResultAccess: SafeAnalysisResultAccess =
    SafeAnalysisResultAccessImpl(projectCache),
  override val codeGeneratorBindings: List<CodeGeneratorBinding> = defaultCodeGeneratorBindings()
) : ProjectCollector {

  val projectDependencyFactory: ProjectDependency.Factory
    get() = RealConfiguredProjectDependencyFactory(
      pathResolver = TypeSafeProjectPathResolver(projectProvider),
      generatorBindings = codeGeneratorBindings
    )

  suspend fun McProjectBuilder<*>.addDependency(
    configurationName: ConfigurationName,
    project: McProject,
    asTestFixture: Boolean = false
  ) {
    val old = projectDependencies[configurationName].orEmpty()

    val newDependency = projectDependencyFactory
      .create(configurationName, project.path, asTestFixture)

    val (newDeclaration, tokenOrNull) = newDependency
      .asDeclaration(this@addDependency)

    addDependency(
      configuredDependency = newDependency,
      newDeclaration = newDeclaration,
      existingMarkerDeclaration = tokenOrNull
    )

    projectDependencies[configurationName] = old + newDependency
  }

  /**
   * @param configuredDependency the dependency model being added
   * @param newDeclaration the text to be added to the project's build file
   * @param existingMarkerDeclaration if not null, the new declaration will be added above or beyond
   *     this declaration. Of all declarations in the `dependencies { ... }` block, this declaration
   *     should be closest to the desired location of the new declaration.
   * @receiver the project to which we're adding a dependency
   * @since 0.12.0
   */
  fun addDependency(
    configuredDependency: ConfiguredDependency,
    newDeclaration: DependencyDeclaration,
    existingMarkerDeclaration: DependencyDeclaration? = null
  ) {

    if (existingMarkerDeclaration != null) {
      prependStatement(
        newDeclaration = newDeclaration,
        existingDeclaration = existingMarkerDeclaration
      )
    } else {
      addStatement(newDeclaration = newDeclaration)
    }

    when (configuredDependency) {
      is ProjectDependency -> projectDependencies.add(configuredDependency)
      is ExternalDependency -> externalDependencies.add(configuredDependency)
    }
  }

  fun run() {

    val root = kotlinProject(":") {

      val lib1 = kotlinProject(":lib1") {
        buildFile {
          """
          plugins {
            kotlin("jvm")
          }
          """.trimIndent()
        }
      }

      val lib2 = kotlinProject(":lib2") {
        addDependency(ConfigurationName.api, lib1)

        buildFile {
          """
          plugins {
            kotlin("jvm")
          }

          dependencies {

          }
          """.trimIndent()
        }
      }
    }
  }
}
