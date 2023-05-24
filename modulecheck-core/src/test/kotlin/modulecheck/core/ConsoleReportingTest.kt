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

package modulecheck.core

import io.kotest.matchers.string.shouldContain
import modulecheck.finding.CouldUseAnvilFinding
import modulecheck.finding.Finding.FindingResult
import modulecheck.finding.Finding.Position
import modulecheck.finding.FindingName
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.runtime.test.RunnerTest
import modulecheck.utils.remove
import org.junit.jupiter.api.Test
import java.io.File

internal class ConsoleReportingTest : RunnerTest() {

  @Test
  fun `zero findings should report '0 issues' to console`() {

    run(
      autoCorrect = false,
      findingFactory = findingFactory()
    )

    logger.collectReport()
      .joinToString()
      .clean()
      .remove("\u200B") shouldBe "ModuleCheck found 0 issues"
  }

  @Test
  fun `one finding should report '1 issue' to console`() {

    run(
      autoCorrect = false,
      findingFactory = findingFactory(
        listOf(
          CouldUseAnvilFinding(
            findingName = FindingName("use-anvil-factory-generation"),
            dependentProject = kotlinProject(":lib1"),
            buildFile = File(testProjectDir, "lib1/build.gradle.kts")
          )
        )
      )
    )

    logger.collectReport()
      .joinToString()
      .clean()
      .remove("\u200B") shouldBe """
          :lib1
                 configuration    dependency                           name                            source    build file
              X                   com.google.dagger:dagger-compiler    use-anvil-factory-generation              /lib1/build.gradle.kts:

      ModuleCheck found 1 issue
    """
  }

  @Test
  fun `multiple findings should report 'n issues' to console`() {

    run(
      autoCorrect = false,
      findingFactory = findingFactory(
        listOf(
          CouldUseAnvilFinding(
            findingName = FindingName("use-anvil-factory-generation"),
            dependentProject = kotlinProject(":lib1"),
            buildFile = File(testProjectDir, "lib1/build.gradle.kts")
          ),
          CouldUseAnvilFinding(
            findingName = FindingName("use-anvil-factory-generation"),
            dependentProject = kotlinProject(":lib2"),
            buildFile = File(testProjectDir, "lib2/build.gradle.kts")
          )
        )
      )
    )

    logger.collectReport()
      .joinToString()
      .clean()
      .remove("\u200B") shouldBe """
        :lib1
               configuration    dependency                           name                            source    build file
            X                   com.google.dagger:dagger-compiler    use-anvil-factory-generation              /lib1/build.gradle.kts:

        :lib2
               configuration    dependency                           name                            source    build file
            X                   com.google.dagger:dagger-compiler    use-anvil-factory-generation              /lib2/build.gradle.kts:

    ModuleCheck found 2 issues
    """
  }

  @Test
  fun `non-zero findings should print suppression advice to console`() {

    run(
      autoCorrect = false,
      findingFactory = findingFactory(
        listOf(
          CouldUseAnvilFinding(
            findingName = FindingName("use-anvil-factory-generation"),
            dependentProject = kotlinProject(":lib1"),
            buildFile = File(testProjectDir, "lib1/build.gradle.kts")
          )
        )
      )
    )

    logger.collectReport()
      .joinToString() shouldContain "To ignore any of these findings, " +
      "annotate the dependency declaration with " +
      """@Suppress("<the name of the issue>") in Kotlin, """ +
      "or //noinspection <the name of the issue> in Groovy.\n" +
      "See https://rbusarow.github.io/ModuleCheck/docs/suppressing-findings for more info."
  }

  @Test
  fun `zero findings should succeed`() {

    run(
      autoCorrect = false,
      findingFactory = findingFactory()
    ).isSuccess shouldBe true
  }

  @Test
  fun `all findings fixed should succeed`() {

    run(
      autoCorrect = false,
      findingFactory = findingFactory(
        listOf(
          CouldUseAnvilFinding(
            findingName = FindingName("use-anvil-factory-generation"),
            dependentProject = kotlinProject(":lib1"),
            buildFile = File(testProjectDir, "lib1/build.gradle.kts")
          )
        )
      ),
      findingResultFactory = { _, _, _ ->
        listOf(
          FindingResult(
            dependentPath = StringProjectPath(":dependentPath"),
            findingName = FindingName("use-anvil-factory-generation"),
            sourceOrNull = "sourceOrNull",
            configurationName = "configurationName",
            dependencyIdentifier = "dependencyIdentifier",
            positionOrNull = Position(1, 2),
            buildFile = File("buildFile"),
            message = "message",
            fixed = true
          )
        )
      }
    ).isSuccess shouldBe true
  }

  @Test
  fun `non-zero unfixed findings should fail`() {

    run(
      autoCorrect = false,
      findingFactory = findingFactory(
        listOf(
          CouldUseAnvilFinding(
            findingName = FindingName("use-anvil-factory-generation"),
            dependentProject = kotlinProject(":lib1"),
            buildFile = File(testProjectDir, "lib1/build.gradle.kts")
          )
        )
      ),
      findingResultFactory = { _, _, _ ->
        listOf(
          FindingResult(
            dependentPath = StringProjectPath(":dependentPath"),
            findingName = FindingName("use-anvil-factory-generation"),
            sourceOrNull = "sourceOrNull",
            configurationName = "configurationName",
            dependencyIdentifier = "dependencyIdentifier",
            positionOrNull = Position(1, 2),
            buildFile = File("buildFile"),
            message = "message",
            fixed = false
          )
        )
      }
    ).isFailure shouldBe true
  }
}
