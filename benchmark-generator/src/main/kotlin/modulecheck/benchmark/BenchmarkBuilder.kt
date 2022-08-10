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
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.ProjectDependency
import modulecheck.model.dependency.TypeSafeProjectPathResolver
import modulecheck.model.dependency.impl.RealConfiguredProjectDependencyFactory
import modulecheck.parsing.kotlin.compiler.impl.SafeAnalysisResultAccess
import modulecheck.parsing.kotlin.compiler.impl.SafeAnalysisResultAccessImpl
import modulecheck.project.ProjectCache
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
        buildFile {
          """
          plugins {
            kotlin("jvm")
          }
          """.trimIndent()
        }

        addDependency(ConfigurationName.api, lib1)
      }
    }
  }
}
