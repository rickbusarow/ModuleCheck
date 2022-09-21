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

package modulecheck.builds.ktlint

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.parent
import com.pinterest.ktlint.core.ast.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtPrimaryConstructor

class NoUselessConstructorKeywordRule : Rule("no-useless-constructor-keyword") {

  override fun beforeVisitChildNodes(
    node: ASTNode,
    autoCorrect: Boolean,
    emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
  ) {

    if (node.elementType == ElementType.CONSTRUCTOR_KEYWORD) {
      val constructorNode = node.parent(ElementType.PRIMARY_CONSTRUCTOR) ?: return

      val constructorPsi = constructorNode.psi as? KtPrimaryConstructor ?: return

      if (constructorPsi.annotations.isEmpty() && constructorPsi.modifierList == null) {

        emit(node.startOffset, "Useless constructor keyword", true)
        val leadingWhitespaceNode = node.prevLeaf(true)!!
        constructorNode.removeChild(node)
        constructorNode.removeChild(leadingWhitespaceNode)
      }
    }
  }
}
