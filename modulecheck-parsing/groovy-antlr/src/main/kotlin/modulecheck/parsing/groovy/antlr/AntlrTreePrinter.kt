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

package modulecheck.parsing.groovy.antlr

import groovyjarjarantlr4.v4.runtime.tree.ParseTree
import groovyjarjarantlr4.v4.runtime.tree.Tree
import modulecheck.utils.traversal.AbstractTreePrinter

internal class AntlrTreePrinter(
  whitespaceChar: Char = ' '
) : AbstractTreePrinter<Tree>(whitespaceChar) {

  override fun Tree.children(): Sequence<Tree> = sequence {
    (0 until childCount).forEach { yield(getChild(it)) }
  }

  override fun Tree.text(): String = (this as ParseTree).text
  override fun Tree.typeName(): String = this::class.java.simpleName
  override fun Tree.parent(): Tree? = parent
  override fun Tree.simpleClassName(): String = this::class.java.simpleName

  companion object {

    internal fun <T : Tree> T.printEverything(whitespaceChar: Char = ' '): T =
      apply { AntlrTreePrinter(whitespaceChar).printTreeString(this) }
  }
}
