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

package modulecheck.parsing.psi

import modulecheck.utils.traversal.AbstractTreePrinter
import org.jetbrains.kotlin.com.intellij.psi.PsiElement

internal class PsiTreePrinter(
  whitespaceChar: Char = ' '
) : AbstractTreePrinter<PsiElement>(whitespaceChar) {

  override fun PsiElement.children(): Sequence<PsiElement> = children.asSequence()
  override fun PsiElement.text(): String = text
  override fun PsiElement.typeName(): String = node.elementType.toString()
  override fun PsiElement.parent(): PsiElement? = parent
  override fun PsiElement.simpleClassName(): String = this::class.java.simpleName

  companion object {

    internal fun <T : PsiElement> T.printEverything(
      whitespaceChar: Char = ' '
    ): T = apply { PsiTreePrinter(whitespaceChar).printTreeString(this) }
  }
}
