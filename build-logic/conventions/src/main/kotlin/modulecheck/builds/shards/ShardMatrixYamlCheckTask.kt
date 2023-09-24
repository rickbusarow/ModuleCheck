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

package modulecheck.builds.shards

import com.rickbusarow.kgx.dependsOn
import modulecheck.builds.BaseYamlMatrixTask
import modulecheck.builds.diffString
import modulecheck.builds.getFinal
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationTask
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File
import javax.inject.Inject

abstract class ShardMatrixYamlCheckTask @Inject constructor(
  objectFactory: ObjectFactory
) : BaseYamlMatrixTask(objectFactory), VerificationTask {

  @get:Input
  abstract val numShards: Property<Int>

  @get:Input
  abstract val updateTaskName: Property<String>

  @TaskAction
  fun check() {
    val ciFile = requireCiFile()

    val ciText = ciFile.readText()
    val pattern = matrixSectionRegex

    val newText = ciText.replace(pattern) { match ->

      val (indent, startTag, _, closingLine) = match.destructured

      val newContent = createYaml(indent, numShards.get())

      "$indent$startTag$newContent$closingLine"
    }

    if (ciText != newText) {
      val message = "The test shard matrix in the CI file is out of date.  " +
        "Run ./gradlew ${updateTaskName.getFinal()} to automatically update." +
        "\n\tfile://${yamlFile.get()}"

      createStyledOutput()
        .withStyle(StyledTextOutput.Style.Description)
        .println(message)

      println()
      println(diffString(ciText, newText))
      println()

      require(false)
    }
  }
}

fun Project.registerYamlShardsTasks(
  shardCount: Int,
  startTagName: String,
  endTagName: String,
  taskNamePart: String,
  yamlFile: File
) {

  require(yamlFile.exists()) {
    "Could not resolve '$yamlFile'."
  }

  val updateTask = tasks.register(
    "${taskNamePart}ShardMatrixYamlUpdate",
    ShardMatrixYamlUpdateTask::class.java
  ) { task ->
    task.yamlFile.set(yamlFile)
    task.numShards.set(shardCount)
    task.startTagProperty.set(startTagName)
    task.endTagProperty.set(endTagName)
  }

  tasks.named("fix").dependsOn(updateTask)

  val checkTask = tasks.register(
    "${taskNamePart}ShardMatrixYamlCheck",
    ShardMatrixYamlCheckTask::class.java
  ) { task ->
    task.yamlFile.set(yamlFile)
    task.numShards.set(shardCount)
    task.startTagProperty.set(startTagName)
    task.endTagProperty.set(endTagName)
    task.updateTaskName.set(updateTask.name)
    task.mustRunAfter(updateTask)
  }

  // Automatically run the check task when running `check`
  tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).dependsOn(checkTask)
}

abstract class ShardMatrixYamlUpdateTask @Inject constructor(
  objectFactory: ObjectFactory
) : BaseYamlMatrixTask(objectFactory) {

  @get:Input
  abstract val numShards: Property<Int>

  @TaskAction
  fun execute() {
    val ciFile = requireCiFile()

    val ciText = ciFile.readText()
    val pattern = matrixSectionRegex

    val newText = ciText.replace(pattern) { match ->

      val (indent, startTag, _, closingLine) = match.destructured

      val newContent = createYaml(indent, numShards.get())

      "$indent$startTag$newContent$closingLine"
    }

    if (ciText != newText) {

      ciFile.writeText(newText)

      val message = "Updated the test shard matrix in the CI file." +
        "\n\tfile://${yamlFile.get()}"

      createStyledOutput()
        .withStyle(StyledTextOutput.Style.Description)
        .println(message)

      println()
      println(diffString(ciText, newText))
      println()
    }
  }
}

private fun createYaml(indent: String, numShards: Int): String {

  val shardList = buildString {
    append("[ ")
    repeat(numShards) {
      val i = it + 1
      append("$i")
      if (i < numShards) append(", ")
    }
    append(" ]")
  }

  return "${indent}shardNum: $shardList\n"
}
