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

package com.rickbusarow.modulecheck.sort

import com.rickbusarow.modulecheck.internal.asKtFile
import com.rickbusarow.modulecheck.parser.DslBlockParser
import com.rickbusarow.modulecheck.parser.PsiElementWithSurroundingText
import com.rickbusarow.modulecheck.task.AbstractModuleCheckTask
import org.gradle.api.tasks.TaskAction
import java.util.*

abstract class SortDependenciesTask : AbstractModuleCheckTask() {

  @TaskAction
  fun run() {
    val parser = DslBlockParser("dependencies")

    project
      .allprojects
      .filter { it.buildFile.exists() }
      .forEach { sub ->

        val result = parser.parse(sub.buildFile.asKtFile()) ?: return@forEach

        val sorted = result
          .elements
          .grouped()
          .joinToString("\n\n") { list ->
            list
              .sortedBy { psiElementWithSurroundings ->
                psiElementWithSurroundings
                  .psiElement
                  .text
                  .toLowerCase(Locale.US)
              }
              .joinToString("\n")
          }
          .trim()

        val allText = sub.buildFile.readText()

        val newText = allText.replace(result.blockText, sorted)

        sub.buildFile.writeText(newText)
      }
  }

  fun List<PsiElementWithSurroundingText>.grouped() = groupBy {
    it
      .psiElement
      .text
      .split("[(.]".toRegex())
      .take(2)
      .joinToString("-")
  }.toSortedMap(compareBy { it.toLowerCase(Locale.US) })
    .map { it.value }
}
