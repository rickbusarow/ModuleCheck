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

package modulecheck.gradle

import io.kotest.inspectors.forAll
import io.kotest.matchers.string.shouldContain
import modulecheck.utils.child
import modulecheck.utils.createSafely
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class TasksValidationTest : BaseGradleTest() {

  @Test
  fun `all tasks with descriptions`() {

    rootBuild.appendText(
      """
      val mcTasks by tasks.registering {
        doLast {
          val message = tasks
            .matching { it.group == "moduleCheck" }
            .sortedBy { it.name }
            .joinToString("\n") { it.name + " - " + it.description }

          println(message)
        }
      }
      """.trimIndent()
    )

    shouldSucceed("mcTasks") withTrimmedMessage """
      moduleCheck - runs all enabled ModuleCheck rules
      moduleCheckAuto - runs all enabled ModuleCheck rules with auto-correct
      moduleCheckDepths - The longest path between this module and its leaf nodes
      moduleCheckGraphs - The longest path between this module and its leaf nodes
      moduleCheckSortDependencies - Sorts all dependencies within a dependencies { ... } block
      moduleCheckSortDependenciesAuto - Sorts all dependencies within a dependencies { ... } block
      moduleCheckSortPlugins - Sorts Gradle plugins which are applied using the plugins { ... } block
      moduleCheckSortPluginsAuto - Sorts Gradle plugins which are applied using the plugins { ... } block
    """
  }

  @TestFactory
  fun `all tasks should ignore configuration caching`() = gradle {

    // "ignore" this test for Gradle versions less than 7.4,
    // since they can't disable caching programmatically
    if (gradleVersion < "7.4") return@gradle

    kotlinProject(":") {
      buildFile {
        """
        plugins {
          id("com.rickbusarow.module-check")
        }
        """
      }

      projectDir.child("settings.gradle.kts").createSafely()
    }

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

      val expected1 = "Configuration cache is an incubating feature."
      val expected2 =
        "Calculating task graph as no configuration cache is available for tasks: $taskName"

      // The first invocation would always succeed, but will generate a cache if caching isn't ignored
      shouldSucceed(taskName, "--configuration-cache").output.clean().let { output ->
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
