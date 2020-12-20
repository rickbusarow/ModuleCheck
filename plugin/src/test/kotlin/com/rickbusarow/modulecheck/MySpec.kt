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
