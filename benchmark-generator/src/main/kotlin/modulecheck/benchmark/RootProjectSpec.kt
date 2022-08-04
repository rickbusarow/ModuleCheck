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
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.ProjectDependency
import modulecheck.model.dependency.TypeSafeProjectPathResolver
import modulecheck.model.dependency.impl.RealConfiguredProjectDependencyFactory
import modulecheck.parsing.gradle.dsl.asDeclaration
import modulecheck.parsing.kotlin.compiler.impl.SafeAnalysisResultAccess
import modulecheck.parsing.kotlin.compiler.impl.SafeAnalysisResultAccessImpl
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.project.gen.ProjectCollector
import java.io.File
import java.nio.charset.Charset

class RootProjectSpec(
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

  suspend fun McProject.addDependency(
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

  fun File.writeText(content: String) {
    writeText(content.trimIndent(), Charset.defaultCharset())
  }
}
