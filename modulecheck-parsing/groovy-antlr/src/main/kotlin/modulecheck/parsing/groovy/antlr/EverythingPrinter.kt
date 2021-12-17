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

package modulecheck.parsing.groovy.antlr

import groovyjarjarantlr4.v4.runtime.tree.ParseTree
import groovyjarjarantlr4.v4.runtime.tree.RuleNode
import org.apache.groovy.parser.antlr4.GroovyParser
import org.apache.groovy.parser.antlr4.GroovyParserBaseVisitor

internal class EverythingPrinter : GroovyParserBaseVisitor<Unit>() {

  private val dashes = "------------------------------------------------------------"

  override fun visit(tree: ParseTree) {

    val parent = tree.parent?.let { it::class.java.simpleName }

    println(
      """ $dashes  ${tree::class.java.simpleName}    -- parent: $parent
      |
      |`${tree.text}`
      |
    """.trimMargin()
    )
    super.visit(tree)
  }

  override fun visitChildren(node: RuleNode) {

    val parent = node.parent?.let { it::class.java.simpleName }

    println(
      """ $dashes  ${node::class.java.simpleName}    -- parent: $parent
      |
      |`${node.text}`
      |
    """.trimMargin()
    )
    super.visitChildren(node)
  }

  override fun visitExpression(ctx: GroovyParser.ExpressionContext) {

    val parent = ctx.parent?.let { it::class.java.simpleName }

    println(
      """ $dashes  ${ctx::class.java.simpleName}    -- parent: $parent
      |
      |`${ctx.text}`
      |
    """.trimMargin()
    )
    super.visitExpression(ctx)
  }
}
