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

package modulecheck.core

import io.kotest.matchers.string.shouldContain
import modulecheck.api.Finding
import modulecheck.api.test.ReportingLogger
import modulecheck.api.test.TestSettings
import modulecheck.core.anvil.CouldUseAnvilFinding
import modulecheck.testing.BaseTest
import org.junit.jupiter.api.Test
import java.io.File

internal class ConsoleReportingTest : BaseTest() {

  val baseSettings by resets { TestSettings() }
  val logger by resets { ReportingLogger() }

  @Test
  fun `zero findings should report '0 issues' to console`() {

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = { listOf() },
      logger = logger
    )

    runner.run(listOf())

    logger.collectReport()
      .joinToString()
      .clean() shouldBe "ModuleCheck found 0 issues"
  }

  @Test
  fun `one finding should report '1 issue' to console`() {

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = {
        listOf(
          CouldUseAnvilFinding(
            buildFile = File(testProjectDir, "lib1/build.gradle.kts"),
            dependentPath = ":lib1"
          )
        )
      },
      logger = logger
    )

    runner.run(listOf())

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
          :lib1
                 dependency                           name                 source    build file
              X  com.google.dagger:dagger-compiler    useAnvilFactories              /lib1/build.gradle.kts:

      ModuleCheck found 1 issue
    """
  }

  @Test
  fun `multiple findings should report 'n issues' to console`() {

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = {
        listOf(
          CouldUseAnvilFinding(
            buildFile = File(testProjectDir, "lib1/build.gradle.kts"),
            dependentPath = ":lib1"
          ),
          CouldUseAnvilFinding(
            buildFile = File(testProjectDir, "lib2/build.gradle.kts"),
            dependentPath = ":lib2"
          )
        )
      },
      logger = logger
    )

    runner.run(listOf())

    logger.collectReport()
      .joinToString()
      .clean() shouldBe """
        :lib1
               dependency                           name                 source    build file
            X  com.google.dagger:dagger-compiler    useAnvilFactories              /lib1/build.gradle.kts:

        :lib2
               dependency                           name                 source    build file
            X  com.google.dagger:dagger-compiler    useAnvilFactories              /lib2/build.gradle.kts:

    ModuleCheck found 2 issues
    """
  }

  @Test
  fun `non-zero findings should print suppression advice to console`() {

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = {
        listOf(
          CouldUseAnvilFinding(
            buildFile = File(testProjectDir, "lib1/build.gradle.kts"),
            dependentPath = ":lib1"
          )
        )
      },
      logger = logger
    )

    runner.run(listOf())

    logger.collectReport()
      .joinToString() shouldContain "To ignore any of these findings, " +
      "annotate the dependency declaration with " +
      "@Suppress(\"<the name of the issue>\") in Kotlin, " +
      "or //noinspection <the name of the issue> in Groovy.\n" +
      "See https://rbusarow.github.io/ModuleCheck/docs/suppressing-findings for more info."
  }

  @Test
  fun `zero findings should succeed`() {

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = { listOf() },
      logger = logger
    )

    runner.run(listOf()).isSuccess shouldBe true
  }

  @Test
  fun `all findings fixed should succeed`() {

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = {
        listOf(
          CouldUseAnvilFinding(
            buildFile = File(testProjectDir, "lib1/build.gradle.kts"),
            dependentPath = ":lib1"
          )
        )
      },
      logger = logger,
      findingResultFactory = { _, _, _ ->
        listOf(
          Finding.FindingResult(
            dependentPath = "dependentPath",
            problemName = "problemName",
            sourceOrNull = "sourceOrNull",
            dependencyPath = "dependencyPath",
            positionOrNull = Finding.Position(1, 2),
            buildFile = File("buildFile"),
            message = "message",
            fixed = true
          )
        )
      }
    )

    runner.run(listOf()).isSuccess shouldBe true
  }

  @Test
  fun `non-zero unfixed findings should fail`() {

    val runner = ModuleCheckRunner(
      autoCorrect = false,
      settings = baseSettings,
      findingFactory = {
        listOf(
          CouldUseAnvilFinding(
            buildFile = File(testProjectDir, "lib1/build.gradle.kts"),
            dependentPath = ":lib1"
          )
        )
      },
      logger = logger,
      findingResultFactory = { _, _, _ ->
        listOf(
          Finding.FindingResult(
            dependentPath = "dependentPath",
            problemName = "problemName",
            sourceOrNull = "sourceOrNull",
            dependencyPath = "dependencyPath",
            positionOrNull = Finding.Position(1, 2),
            buildFile = File("buildFile"),
            message = "message",
            fixed = false
          )
        )
      }
    )

    runner.run(listOf()).isFailure shouldBe true
  }
}
