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

package modulecheck.gradle.task

import org.gradle.api.tasks.TaskAction

open class ModuleCheckDependencyResolutionTask : AbstractModuleCheckTask() {
  init {
    description = "Resolves all external dependencies"
  }

  @TaskAction
  fun execute() {

    val workingDir = "/Users/rbusarow/Development/ModuleCheck/modulecheck-gradle/plugin/" +
      "build/tests/UnusedDependenciesPluginTest/" +
      "module_with_a_declaration_used_in_an_android_module_with_kotlin_source_" +
      "directory_should_not_be_unused/[gradle_7.5.1,_agp_7.3.0,_anvil_2.4.2,_kotlin_1.7.20-RC]"

    // dependsOn.filterIsInstance<GradleConfiguration>()
    //   .filterNot { it.name.endsWith("RuntimeElements") }
    //   .forEach {
    //     it.resolve()
    //       .sorted()
    //       .joinToString("\n")
    //       .also(::println)
    //   }

    inputs.files.files
      .sorted()
      .flatMap { f ->
        if (f.isDirectory) {
          f.walkBottomUp().filter { it.isFile }.toList()
        } else {
          listOf(f)
        }
      }
      .joinToString("\n") { it.absolutePath.removePrefix(workingDir) }
      .also(::println)

    // project.configurations.getByName("moduleCheckDebugAggregateDependencies")

    // .allDependencies
    // .flatMap {
    //   when (it) {
    //     is DefaultSelfResolvingDependency -> it.resolve()
    //     is DefaultExternalModuleDependency -> it.artifacts
    //     else -> emptyList()
    //   }
    // }
    // .filterIsInstance<DefaultSelfResolvingDependency>()
    // .flatMap { it.resolve() }
    // .joinToString("\n")

    // println("###################################### depends on")
    // dependsOn.joinToString("\n")
    //   .also(::println)
    //
    // println("###################################### input files")
    // inputs.files.joinToString("\n")
    //   .also(::println)
    //
    // dependsOn.filterIsInstance<GradleConfiguration>()
    //   .forEach {
    //
    //     println("###################################### files  ${it.name}")
    //     it.dependencies.joinToString("\n").also(::println)
    //   }
  }
}
