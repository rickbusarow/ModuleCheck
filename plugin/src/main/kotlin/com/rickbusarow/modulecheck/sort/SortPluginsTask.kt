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

abstract class SortPluginsTask : AbstractModuleCheckTask() {

  @TaskAction
  fun run() {
    val parser = DslBlockParser("plugins")

    project
      .allprojects
      .filter { it.buildFile.exists() }
      .forEach { sub ->

        val result = parser.parse(sub.buildFile.asKtFile()) ?: return@forEach

        val patterns = listOf(
          """id\("com\.android.*"\)""",
          """id\("android-.*"\)""",
          """id\("java-library"\)""",
          """kotlin\("jvm"\)""",
          """android.*""",
          """javaLibrary.*""",
          """kotlin.*""",
          """id.*"""
        )

        val comparables: Array<(PsiElementWithSurroundingText) -> Comparable<*>> = patterns
          .map { it.toRegex() }
          .map { regex ->
            { str: String -> !str.matches(regex) }
          }
          .map { booleanLambda ->
            { psi: PsiElementWithSurroundingText ->

              booleanLambda.invoke(psi.psiElement.text)
            }
          }.toTypedArray()

        @Suppress("SpreadOperator")
        val comparator = compareBy(*comparables)

        val sorted = result
          .elements
          .sortedWith(comparator)
          .joinToString("\n")
          .trim()

        val allText = sub.buildFile.readText()

        val newText = allText.replace(result.blockText, sorted)

        sub.buildFile.writeText(newText)
      }
  }
}
