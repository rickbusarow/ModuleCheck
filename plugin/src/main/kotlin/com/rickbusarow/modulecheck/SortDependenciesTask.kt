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

import com.rickbusarow.modulecheck.internal.asKtFile
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import java.util.*

abstract class SortDependenciesTask : DefaultTask() {

  @TaskAction
  fun run() {

    project.allprojects.forEach { sub ->
      if (sub.buildFile.exists()) {

        val visitor = GradleDependencyVisitor()
        sub
          .buildFile
          .asKtFile()
          .accept(visitor)

        val sorted = visitor
          .things
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

        visitor.blockText?.let {
          val newText = allText.replace(it, sorted)

          sub.buildFile.writeText(newText)
        }
      }
    }
  }
}

class GradleDependencyVisitor : KtTreeVisitorVoid() {

  val things = mutableListOf<PsiElementWithSurroundings>()
  var blockText: String? = null
  var blockWhiteSpace: String? = null

  override fun visitCallExpression(expression: KtCallExpression) {

    if (expression.text.startsWith("dependencies {")) {

      val visitor = DependencyBlockDeclarationVisitor()

      expression.findDescendantOfType<KtBlockExpression>()?.let {

        blockWhiteSpace = (it.prevSibling as? PsiWhiteSpace)?.text?.trimStart('\n', '\r')
        visitor.visitBlockExpression(it)
      }
    }
  }

  inner class DependencyBlockDeclarationVisitor : KtTreeVisitorVoid() {

    override fun visitBlockExpression(expression: KtBlockExpression) {

      blockText = expression.text

      val visited = mutableSetOf<PsiElement>()

      val elements = expression
        .children
        .filterNot { it is PsiComment || it is PsiWhiteSpace }
        .filterIsInstance<PsiElement>()
        .mapIndexed { index, psi ->
          if (index == 0) {
            psi.withSurroundings(visited, blockWhiteSpace ?: "")
          } else {
            psi.withSurroundings(visited)
          }
        }

      things.addAll(elements)
    }
  }
}

fun List<PsiElementWithSurroundings>.grouped() = groupBy {
  it
    .psiElement
    .text
    .split("[(.]".toRegex())
    .take(2)
    .joinToString("-")
}.toSortedMap(compareBy { it.toLowerCase(Locale.US) })
  .map { it.value }

data class PsiElementWithSurroundings(
  val psiElement: PsiElement,
  val previousText: String,
  val nextText: String
) {
  override fun toString(): String {
    return previousText + psiElement.text + nextText
  }
}

fun PsiElement.withSurroundings(
  visited: MutableSet<PsiElement>,
  startingWhitespace: String = ""
): PsiElementWithSurroundings {

  var previous: PsiElement? = prevSibling

  val prevStrings = mutableListOf<String>()

  while (previous !in visited && (previous is PsiWhiteSpace || previous is PsiComment)) {

    visited.add(previous)

    prevStrings.add(previous.text)
    previous = previous.prevSibling
  }

  val previousText = prevStrings
    .reversed()
    .joinToString("")

  var next: PsiElement? = nextSibling

  var nextText = ""

  while (next is PsiWhiteSpace || next is PsiComment) {

    if ((text + nextText + next.text).lines().size == 1) {
      visited.add(next)
      nextText += next.text
      next = next.nextSibling
    } else {
      break
    }
  }

  return PsiElementWithSurroundings(
    psiElement = this,
    previousText = previousText.trimStart('\n', '\r') + startingWhitespace,
    nextText = nextText.trimEnd()
  )
}
