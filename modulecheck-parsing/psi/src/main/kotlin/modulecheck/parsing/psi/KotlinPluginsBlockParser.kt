/*
 * Copyright (C) 2021-2022 Rick Busarow
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

package modulecheck.parsing.psi

import modulecheck.parsing.psi.internal.getChildrenOfTypeRecursive
import modulecheck.parsing.psi.internal.nameSafe
import modulecheck.reporting.logging.McLogger
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import javax.inject.Inject

class KotlinPluginsBlockParser @Inject constructor(
  private val logger: McLogger
) {

  @Suppress("ReturnCount")
  fun parse(file: KtFile): KotlinPluginsBlock? {

    var block: KotlinPluginsBlock? = null

    file.getChildrenOfTypeRecursive<KtCallExpression>()
      .firstOrNull { it.nameSafe() == "plugins" }
      ?.let { fullBlock ->

        val blockSuppressed = (fullBlock.parent as? KtAnnotatedExpression)
          ?.suppressedNames()
          .orEmpty()

        val fullText = fullBlock.text

        val contentBlock = fullBlock.findDescendantOfType<KtBlockExpression>()
          ?: return@let null

        val contentString = contentBlock.text

        val blockWhiteSpace = (contentBlock.prevSibling as? PsiWhiteSpace)?.text
          ?.trimStart('\n', '\r')
          ?: ""

        val pluginsBlock = KotlinPluginsBlock(
          logger = logger,
          fullText = fullText,
          lambdaContent = blockWhiteSpace + contentString,
          blockSuppressed = blockSuppressed
        )

        contentBlock.children
          .forEach { element ->

            when (element) {
              is KtAnnotatedExpression -> {
                val suppressed = element.suppressedNames()

                val parsedString = element.getChildOfType<KtCallExpression>()
                  ?.text
                  ?: element.getChildOfType<KtNameReferenceExpression>()?.text
                  ?: element.text

                pluginsBlock.addStatement(
                  parsedString = parsedString,
                  suppressed = suppressed + blockSuppressed
                )
              }
              is KtBinaryExpression,
              is KtCallExpression,
              is KtNameReferenceExpression -> {

                pluginsBlock.addStatement(
                  parsedString = element.text,
                  suppressed = blockSuppressed
                )
              }
            }
          }

        block = pluginsBlock
      }

    return block
  }
}
