/*
 * Copyright (C) 2020 Rick Busarow
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

package com.rickbusarow.modulecheck

import com.rickbusarow.modulecheck.testing.newFile
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

class MySpec : FreeSpec({

  val testProjectDir = tempDir()

  val settingsFile = testProjectDir.newFile("settings.gradle.kts")
  val buildFile = testProjectDir.newFile("build.gradle.kts")

  "test" - {

    settingsFile.writeText(
      """
            rootProject.name = "hello-world"
      """.trimIndent()
    )

    "test helloWorld task" {

      buildFile.writeText(
        """
            tasks.register("helloWorld") {
                doLast {
                    println("Hello world!")
                }
            }
        """.trimIndent()
      )

      val result = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withArguments("helloWorld")
        .build()

      result.output shouldContain "Hello world!"
      result.task(":helloWorld")?.outcome shouldBe TaskOutcome.SUCCESS
    }
  }
})
