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

package modulecheck.builds

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.internal.classpath.Instrumented.systemProperty

abstract class TestConventionPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    target.tasks.withType(Test::class.java).configureEach { task ->
      task.maxHeapSize = "1g"
      task.useJUnitPlatform()

      task.testLogging {
        it.events = setOf(FAILED)
        it.exceptionFormat = TestExceptionFormat.FULL
        it.showExceptions = true
        it.showCauses = true
        it.showStackTraces = true
      }

      target.properties
        .asSequence()
        .filter { (key, value) ->
          key.startsWith("modulecheck") && value != null
        }
        .forEach { (key, value) ->
          systemProperty(key, value as String)
        }

      val ci = System.getenv("CI")?.toBoolean() == true
      if (ci) {
        // defaults to 512m.
        task.maxHeapSize = "1g"
        // Allow JUnit4 tests to run in parallel
        task.maxParallelForks = 1
      } else {
        task.maxHeapSize = "4g"

        task.systemProperties.putAll(
          mapOf(

            // auto-discover and apply any Junit5 extensions in the classpath
            "junit.jupiter.extensions.autodetection.enabled" to true,

            // remove parentheses from test display names
            "junit.jupiter.displayname.generator.default" to
              "org.junit.jupiter.api.DisplayNameGenerator\$Simple",

            // single class instance for all tests
            "junit.jupiter.testinstance.lifecycle.default" to "per_class",

            // https://junit.org/junit5/docs/snapshot/user-guide/#writing-tests-parallel-execution-config-properties
            // Allow unit tests to run in parallel
            "junit.jupiter.execution.parallel.enabled" to true,
            "junit.jupiter.execution.parallel.mode.default" to "concurrent",
            "junit.jupiter.execution.parallel.mode.classes.default" to "concurrent",

            "junit.jupiter.execution.parallel.config.strategy" to "dynamic",
            "junit.jupiter.execution.parallel.config.dynamic.factor" to 1.0
          )
        )

        // Allow JUnit4 tests to run in parallel
        task.maxParallelForks = Runtime.getRuntime().availableProcessors()
      }
    }

    if (target.isRootProject()) {

      val shardCount = 4
      val shardTasks = (1..shardCount)
        .map { index -> target.tasks.register("testShard$index", Test::class.java) }

      // Assign each project to a shard
      val shardAssignments by lazy {

        val testAnnotationRegex = "@Test(?!Factory)".toRegex()
        val testFactoryAnnotationRegex = "@TestFactory".toRegex()

        // Calculate the cost of each project's tests
        val projectTestCosts = target.subprojects
          .associateWith { project ->
            project.file("src/test")
              .walkTopDown()
              .filter { it.isFile && it.extension == "kt" }
              .sumOf { file ->
                val fileText = file.readText()
                val testAnnotationCount = testAnnotationRegex.findAll(fileText).count()
                val testFactoryAnnotationCount =
                  testFactoryAnnotationRegex.findAll(fileText).count()
                testAnnotationCount + (testFactoryAnnotationCount * 2)
              }
          }

        // Sort the projects by descending test cost
        val sortedProjects = projectTestCosts.keys
          .sortedByDescending { projectTestCosts[it] }

        var shardIndex = 0

        sortedProjects.groupBy { shardIndex++ % shardCount }
      }

      // Add dependencies to the shard tasks
      shardTasks.forEachIndexed { shardIndex, shardTask ->

        shardTask.configure { task ->

          val assignedTests = shardAssignments.getValue(shardIndex)
            .map { project -> project.tasks.matchingName("test") }

          task.dependsOn(assignedTests)
        }
      }
    }
  }
}
