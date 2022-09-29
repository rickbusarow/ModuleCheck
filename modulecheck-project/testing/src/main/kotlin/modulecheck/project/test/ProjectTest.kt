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

package modulecheck.project.test

import modulecheck.config.CodeGeneratorBinding
import modulecheck.config.internal.defaultCodeGeneratorBindings
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.ProjectDependency
import modulecheck.model.dependency.TypeSafeProjectPathResolver
import modulecheck.model.dependency.impl.RealConfiguredProjectDependencyFactory
import modulecheck.parsing.kotlin.compiler.impl.DependencyModuleDescriptorAccess
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.project.generation.ProjectCollector
import modulecheck.testing.BaseTest
import java.io.File
import java.nio.charset.Charset

abstract class ProjectTest : BaseTest(), ProjectCollector {

  override val projectCache: ProjectCache by resets { ProjectCache() }
  override val dependencyModuleDescriptorAccess: DependencyModuleDescriptorAccess by resets {
    DependencyModuleDescriptorAccess(projectCache)
  }

  override val root: File
    get() = testProjectDir

  override val codeGeneratorBindings: List<CodeGeneratorBinding>
    get() = defaultCodeGeneratorBindings()

  val projectDependencyFactory: ProjectDependency.Factory
    get() = RealConfiguredProjectDependencyFactory(
      pathResolver = TypeSafeProjectPathResolver(projectProvider),
      generatorBindings = codeGeneratorBindings
    )

  fun McProject.addDependency(
    configurationName: ConfigurationName,
    project: McProject,
    asTestFixture: Boolean = false
  ) {
    val old = projectDependencies[configurationName].orEmpty()

    val cpd =
      projectDependencyFactory.create(configurationName, project.projectPath, asTestFixture)

    projectDependencies[configurationName] = old + cpd
  }

  fun File.writeText(content: String) {
    writeText(content.trimIndent(), Charset.defaultCharset())
  }
}
