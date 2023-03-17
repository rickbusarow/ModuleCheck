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

package modulecheck.builds.ktlint.rules

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.nextLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

class NoDuplicateCopyrightHeaderRule : Rule("no-duplicate-copyright-header") {

  override fun beforeVisitChildNodes(
    node: ASTNode,
    autoCorrect: Boolean,
    emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
  ) {

    if (node.elementType == ElementType.FILE) {

      node.children()
        .filter { it.isCopyrightHeader() }
        .drop(1)
        .forEach { commentNode ->

          val trailingWhitespace = commentNode.nextLeaf(includeEmpty = true)

          emit(commentNode.startOffset, "duplicate copyright header", true)
          node.removeChild(commentNode)
          if (trailingWhitespace != null) {
            node.removeChild(trailingWhitespace)
          }
        }
    }
  }
}
