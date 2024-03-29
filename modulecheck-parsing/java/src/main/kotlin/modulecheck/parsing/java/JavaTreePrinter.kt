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

package modulecheck.parsing.java

import com.github.javaparser.ast.Node
import modulecheck.utils.getOrNull
import modulecheck.utils.traversal.AbstractTreePrinter

internal class JavaTreePrinter(
  whitespaceChar: Char = ' '
) : AbstractTreePrinter<Node>(whitespaceChar) {

  override fun Node.children(): Sequence<Node> = childNodes.asSequence()
  override fun Node.text(): String = toString()
  override fun Node.typeName(): String = this::class.simpleName ?: "----"
  override fun Node.parent(): Node? = parentNode.getOrNull()
  override fun Node.simpleClassName(): String = this::class.java.simpleName

  companion object {

    internal fun <T : Node> T.printEverything(whitespaceChar: Char = ' '): T =
      apply { JavaTreePrinter(whitespaceChar).printTreeString(this) }
  }
}
