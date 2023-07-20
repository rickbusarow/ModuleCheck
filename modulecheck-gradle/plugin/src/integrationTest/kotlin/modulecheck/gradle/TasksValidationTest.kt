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

package modulecheck.gradle

import io.kotest.inspectors.forAll
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class TasksValidationTest : BaseGradleTest() {

  @Test
  fun `all tasks with descriptions`() = test {

    rootBuild.appendText(
      """

      val mcTasks by tasks.registering {
        doLast {
          val message = tasks
            .matching { it.group == "ModuleCheck" }
            .sortedBy { it.name }
            .joinToString("\n") { it.name + " - " + it.description }

          println(message)
        }
      }
      """.trimIndent()
    )

    shouldSucceed(
      "mcTasks",
      withPluginClasspath = true
    ) withTrimmedMessage """
      moduleCheck - runs all enabled ModuleCheck rules
      moduleCheckAuto - runs all enabled ModuleCheck rules with auto-correct
      moduleCheckDepths - runs the project-depth ModuleCheck rule
      moduleCheckGraphs - runs the project-depth ModuleCheck rule
      moduleCheckSortDependencies - runs the sort-dependencies ModuleCheck rule
      moduleCheckSortDependenciesAuto - runs the sort-dependencies ModuleCheck rule with auto-correct
      moduleCheckSortPlugins - runs the sort-plugins ModuleCheck rule
      moduleCheckSortPluginsAuto - runs the sort-plugins ModuleCheck rule with auto-correct
    """
  }

  @TestFactory
  fun `all tasks should ignore configuration caching`() = factory {

    listOf(
      "moduleCheck",
      "moduleCheckAuto",
      "moduleCheckDepths",
      "moduleCheckGraphs",
      "moduleCheckSortDependencies",
      "moduleCheckSortDependenciesAuto",
      "moduleCheckSortPlugins",
      "moduleCheckSortPluginsAuto"
    ).forAll { taskName ->

      val expected1 = if (gradleVersion < "8.1") {
        "Configuration cache is an incubating feature."
      } else {
        "Encryption of the configuration cache is enabled."
      }
      val expected2 =
        "Calculating task graph as no configuration cache is available for tasks: $taskName"

      // The first invocation would always succeed, but will generate a cache if caching isn't ignored
      shouldSucceed(
        taskName,
        "--configuration-cache",
        withPluginClasspath = true
      ).output.clean().let { output ->
        output shouldContain expected1
        output shouldContain expected2
      }

      // The second invocation will fail if a cache exists and caching isn't ignored.
      shouldSucceed(taskName, "--configuration-cache").output.clean().let { output ->
        output shouldContain expected1
        output shouldContain expected2
      }
    }
  }
}
