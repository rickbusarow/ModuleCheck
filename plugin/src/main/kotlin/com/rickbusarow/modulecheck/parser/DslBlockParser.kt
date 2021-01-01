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

package com.rickbusarow.modulecheck.parser

import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType

class DslBlockParser(private val blockName: String) {

  data class ParseResult(
    val elements: List<PsiElementWithSurroundingText>,
    val blockText: String,
    val blockWhiteSpace: String
  )

  @Suppress("ReturnCount")
  fun parse(file: KtFile): ParseResult? {
    val elements = mutableListOf<PsiElementWithSurroundingText>()
    var blockText: String? = null
    var blockWhiteSpace: String? = null

    val blockVisitor = object : KtTreeVisitorVoid() {

      override fun visitBlockExpression(expression: KtBlockExpression) {
        blockText = expression.text

        val visited = mutableSetOf<PsiElement>()

        elements.addAll(
          expression
            .children
            .filterNot { it is PsiComment || it is PsiWhiteSpace }
            .filterIsInstance<PsiElement>()
            .mapIndexed { index, psi ->
              if (index == 0) {
                psi.withSurroundingText(visited, blockWhiteSpace ?: "")
              } else {
                psi.withSurroundingText(visited)
              }
            }
        )
      }
    }

    val callVisitor = object : KtTreeVisitorVoid() {
      override fun visitCallExpression(expression: KtCallExpression) {
        if (expression.text.startsWith("$blockName {")) {
          expression.findDescendantOfType<KtBlockExpression>()?.let {
            blockWhiteSpace = (it.prevSibling as? PsiWhiteSpace)?.text?.trimStart('\n', '\r')
            blockVisitor.visitBlockExpression(it)
          }
        }
      }
    }

    file.accept(callVisitor)

    val text = blockText ?: return null
    val whiteSpace = blockWhiteSpace ?: return null

    return ParseResult(elements, text, whiteSpace)
  }

  fun PsiElement.withSurroundingText(
    visited: MutableSet<PsiElement>,
    startingWhitespace: String = ""
  ): PsiElementWithSurroundingText {
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

    return PsiElementWithSurroundingText(
      psiElement = this,
      previousText = (startingWhitespace + previousText).trimStart('\n', '\r'),
      nextText = nextText.trimEnd()
    )
  }
}

data class PsiElementWithSurroundingText(
  val psiElement: PsiElement,
  val previousText: String,
  val nextText: String
) {
  override fun toString(): String {
    return previousText + psiElement.text + nextText
  }
}
