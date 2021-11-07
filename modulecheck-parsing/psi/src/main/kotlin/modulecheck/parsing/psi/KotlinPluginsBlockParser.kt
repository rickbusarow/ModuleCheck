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

package modulecheck.parsing.psi

import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.callExpressionRecursiveVisitor
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType

class KotlinPluginsBlockParser {

  @Suppress("ReturnCount")
  fun parse(file: KtFile): KotlinPluginsBlock? {
    var blockWhiteSpace: String? = null

    var block: KotlinPluginsBlock? = null

    val blockVisitor = blockExpressionRecursiveVisitor { blockExpression ->

      val pluginsBlock = KotlinPluginsBlock((blockWhiteSpace ?: "") + blockExpression.text)

      // Different plugin declarations initially parse as different types,
      // like KtCallExpression, KtBinaryExpression, or KtNamedReference.
      // The only thing that they all have in common is that they're all direct children
      // of the KtBlockExpression.
      // Anything which isn't a comment or whitespace must be a new plugin declaration
      blockExpression
        .children
        .filterNot { it is PsiComment || it is PsiWhiteSpace }
        .forEach { statement ->

          pluginsBlock.addStatement(
            parsedString = statement.text
          )
        }

      block = pluginsBlock
    }

    val callVisitor = callExpressionRecursiveVisitor { expression ->
      if (expression.text.matches(""".*plugins\s*\{[\s\S]*""".toRegex())) {
        expression.findDescendantOfType<KtBlockExpression>()?.let {
          blockWhiteSpace = (it.prevSibling as? PsiWhiteSpace)?.text?.trimStart('\n', '\r')
          blockVisitor.visitBlockExpression(it)
        }
      }
    }

    file.accept(callVisitor)

    return block
  }
}
