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

import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestLogEvent
import kotlin.math.ceil

abstract class ShardTestTask : Test() {

  @get:Input
  abstract val totalShards: Property<Int>

  @get:Input
  abstract val shardNumber: Property<Int>

  private var filterWasSet: Boolean = false

  fun setFilter() {

    // Calculate the range of test classes for this shard
    val testClassCount = testClassesDirs.asFileTree.matching {
      include("**/*Test.class")
    }.files.size

    val testsPerShard = ceil(testClassCount.toDouble() / totalShards.get()).toInt()
    val startIndex = testsPerShard * (shardNumber.get() - 1)
    val endIndex = minOf(testClassCount, startIndex + testsPerShard)

    testLogging.events(
      TestLogEvent.FAILED,
      TestLogEvent.STARTED,
      TestLogEvent.PASSED,
      TestLogEvent.SKIPPED
    )

    testClassesDirs.asFileTree.matching {
      include("**/*Test.class")
    }.files.asSequence()
      .sorted()
      .map { file -> file.name.replace(".class", "") }
      .drop(startIndex)
      .take(endIndex - startIndex)
      .also {

        println(
          "###### integration test shard ${shardNumber.get()} of ${totalShards.get()} includes:\n" +
            it.joinToString("\n")
        )
      }
      .forEach {
        this@ShardTestTask.filter.includeTest(it, null)
      }

    filterWasSet = true
  }

  @TaskAction
  fun execute() {

    if (!filterWasSet) {
      throw GradleException("This shard test task did not have its filter set.")
    }
  }
}
