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

import modulecheck.utils.createSafely
import modulecheck.utils.resolve
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.TestFactory

class TaskLifecycleTest : BaseGradleTest() {

  @TestFactory
  fun `the modulecheck task should be invoked for the base plugin check task`() = factory {

    kotlinProject(":") {
      buildFile {
        """
        plugins {
          id("com.rickbusarow.module-check")
          base
        }
        """
      }

      projectDir.resolve("settings.gradle.kts").createSafely()
    }

    shouldSucceed(
      "check",
      withPluginClasspath = true
    ) {

      task(":moduleCheck")!!.outcome shouldBe SUCCESS

      tasks shouldBe listOf(
        task(":moduleCheck"),
        task(":check")
      )
    }
  }

  @TestFactory
  fun `the modulecheck task should be invoked for a late, manually created check task`() = factory {

    kotlinProject(":") {
      buildFile {
        """
        plugins {
          id("com.rickbusarow.module-check")
        }

        afterEvaluate {
          task("check")
        }
        """
      }

      projectDir.resolve("settings.gradle.kts").createSafely()
    }

    shouldSucceed(
      "check",
      withPluginClasspath = true
    ) {

      task(":moduleCheck")!!.outcome shouldBe SUCCESS

      tasks shouldBe listOf(
        task(":moduleCheck"),
        task(":check")
      )
    }
  }
}
