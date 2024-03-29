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

import groovyjarjarantlr4.v4.runtime.CharStream
import groovyjarjarantlr4.v4.runtime.ParserRuleContext
import groovyjarjarantlr4.v4.runtime.RuleContext
import groovyjarjarantlr4.v4.runtime.misc.Interval
import groovyjarjarantlr4.v4.runtime.tree.RuleNode
import groovyjarjarantlr4.v4.runtime.tree.Tree
import modulecheck.utils.traversal.Traversals
import org.apache.groovy.parser.antlr4.GroovyParser.AssignmentExprAltContext
import org.apache.groovy.parser.antlr4.GroovyParser.BlockStatementContext
import org.apache.groovy.parser.antlr4.GroovyParser.BlockStatementsContext
import org.apache.groovy.parser.antlr4.GroovyParser.BlockStatementsOptContext
import org.apache.groovy.parser.antlr4.GroovyParser.ClosureContext
import org.apache.groovy.parser.antlr4.GroovyParser.ClosureOrLambdaExpressionContext
import org.apache.groovy.parser.antlr4.GroovyParser.CommandExpressionContext
import org.apache.groovy.parser.antlr4.GroovyParser.GroovyParserRuleContext
import org.apache.groovy.parser.antlr4.GroovyParser.IdentifierPrmrAltContext
import org.apache.groovy.parser.antlr4.GroovyParser.NamePartContext
import org.apache.groovy.parser.antlr4.GroovyParser.NlsContext
import org.apache.groovy.parser.antlr4.GroovyParser.PathElementContext
import org.apache.groovy.parser.antlr4.GroovyParser.PathExpressionContext
import org.apache.groovy.parser.antlr4.GroovyParser.PostfixExprAltContext
import org.apache.groovy.parser.antlr4.GroovyParser.PostfixExpressionContext
import org.apache.groovy.parser.antlr4.GroovyParser.ScriptStatementContext
import org.apache.groovy.parser.antlr4.GroovyParser.ScriptStatementsContext
import org.apache.groovy.parser.antlr4.GroovyParser.SepContext
import org.apache.groovy.parser.antlr4.GroovyParserBaseVisitor

internal inline fun <reified T> RuleContext.parentOfType(): T? {
  return generateSequence(this as? RuleContext?) { it.parent }
    .filterIsInstance<T>()
    .firstOrNull()
}

/**
 * @return a sequence of child nodes of this [Tree] in depth-first
 *   order. The sequence starts with the first child node of this [Tree],
 *   followed by the first child node of the first child node, and so on.
 */
fun Tree.childrenDepthFirst(): Sequence<Tree> {
  return Traversals.depthFirstTraversal(this) { children().toList() }
}

/**
 * @return a sequence of child nodes of type [T] of this [Tree] in depth-first
 *   order. The sequence starts with the first child node of this [Tree],
 *   followed by the first child node of the first child node, and so on.
 */
inline fun <reified T : Tree> Tree.childrenOfTypeDepthFirst(): Sequence<T> {
  return Traversals.depthFirstTraversal(this) { children().toList() }
    .filterIsInstance<T>()
}

/**
 * @param predicate stops visiting child nodes of the given node once this predicate returns false
 * @return a sequence of child nodes of this [Tree] in depth-first order that satisfy
 *   the given [predicate]. The sequence starts with the first child node of this
 *   [Tree], followed by the first child node of the first child node, and so on.
 */
fun Tree.childrenDepthFirst(predicate: (Tree) -> Boolean): Sequence<Tree> =
  Traversals.depthFirstTraversal(this) { children().filter(predicate).toList() }

/**
 * @return a sequence of child nodes of type [T] of this [Tree] in breadth-first
 *   order. The sequence starts with the first child node of this [Tree],
 *   followed by the first child node of the second child node, and so on.
 */
inline fun <reified T : Tree> Tree.childrenOfTypeBreadthFirst(): Sequence<T> {
  return Traversals.breadthFirstTraversal(this) { children().toList() }
    .filterIsInstance<T>()
}

/**
 * @return a sequence of child nodes of this [Tree] in breadth-first
 *   order. The sequence starts with all the child nodes of this [Tree],
 *   followed by all the child nodes of the first child node, and so on.
 */
fun Tree.childrenBreadthFirst(): Sequence<Tree> {
  return Traversals.breadthFirstTraversal(this) { children().toList() }
}

/**
 * @param [predicate] stops visiting child nodes of the parent
 *   of the given node once this predicate returns false
 * @return a sequence of child nodes of this [Tree] in breadth-first order that
 *   satisfy the given [predicate]. The sequence starts with all the child nodes of
 *   this [Tree], followed by all the child nodes of the first child node, and so on.
 */
fun Tree.childrenBreadthFirst(predicate: (Tree) -> Boolean): Sequence<Tree> =
  Traversals.breadthFirstTraversal(this) { children().filter(predicate).toList() }

/** @return a sequence of the children [Tree] nodes. */
fun Tree.children(): Sequence<Tree> = sequence {
  for (it in 0 until childCount) {
    yield(getChild(it))
  }
}

internal inline fun <reified T> ParserRuleContext.childrenOfType(): List<T> {
  return children.filterIsInstance<T>()
}

internal inline fun <reified T> RuleNode.previousSibling(): T? {

  val siblings = (parent as? ParserRuleContext)?.children
    ?.takeIf { it.count() > 1 }
    ?: return null

  val indexInSiblings = siblings.indexOf(this).takeIf { it >= 1 } ?: return null

  return siblings[indexInSiblings - 1] as? T
}

internal fun RuleNode.precedingCommentNodeOrNull(): GroovyParserRuleContext? {

  // First, try going all the way up to the parent scope -- top-level or the entire lambda content
  // This works if this receiver is the first statement within that scope
  val asBlockOrScriptStatement = this as? BlockStatementContext
    ?: this as? ScriptStatementsContext
    ?: (this as? RuleContext)?.parentOfType<BlockStatementContext>()
    ?: (this as? RuleContext)?.parentOfType<ScriptStatementContext>()
    ?: return null

  val scopeAttempt = asBlockOrScriptStatement.previousSibling<SepContext>()
    ?: asBlockOrScriptStatement.previousSibling<NlsContext>()

  if (scopeAttempt != null) return scopeAttempt

  // If the first "scoped" attempt didn't work, then try whatever the hell this is.  Multiple
  // statements inside a lambda get weirdly stacked and grouped.  Sometimes there are several
  // statements in one node with a parent of `BlockStatementsOptContext`, and then a preceding
  // comment is a sibling with that same parent.  Even if there's another comment inside that
  // grouped node.
  val asBlockStatementsOpt = asBlockOrScriptStatement.parentOfType<BlockStatementsOptContext>()
    ?: return null

  return asBlockStatementsOpt.previousSibling<SepContext>()
    ?: asBlockStatementsOpt.previousSibling<NlsContext>()
}

internal inline fun <reified T : RuleContext> ParserRuleContext.childOfType(): T? {
  return childrenOfType<T>().let { ts ->

    require(ts.size in 0..1) {
      """node `$text` has more than one child of type ${T::class.java}.
        |
        |matching children:
        |${ts.joinToString("\n\n") { "-\n" + it.text }}
        |
        |If you don't care about ignoring multiple results, use `.childrenOfType<T>().firstOrNull()` instead.
      """.trimMargin()
    }

    ts.singleOrNull()
  }
}

internal fun ParserRuleContext.originalText(stream: CharStream): String {
  return if (start == null || stop == null) {
    // Weird edge cases around the very top and bottom of the parse tree
    ""
  } else {
    stream.getText(Interval(start.startIndex, stop.stopIndex))
  }
}

internal fun CommandExpressionContext.pathExpression(): PathExpressionContext? {

  val root: PostfixExprAltContext? = childOfType()
    ?: childOfType<AssignmentExprAltContext>()
      ?.childOfType()

  return root?.childOfType<PostfixExpressionContext>()
    ?.childOfType()
}

internal fun CommandExpressionContext.isNamed(name: String): Boolean {

  return pathExpression()
    ?.childOfType<IdentifierPrmrAltContext>()
    ?.identifier()
    ?.text
    ?.trim() == name
}

internal fun CommandExpressionContext.lambdaBlock(): ClosureOrLambdaExpressionContext? {

  return pathExpression()
    ?.childrenOfType<PathElementContext>()
    ?.singleOrNull()
    ?.childOfType()
}

internal fun PathExpressionContext.isNamed(name: String): Boolean {

  return pathElement()
    ?.firstOrNull()
    ?.childrenOfType<NamePartContext>()
    ?.singleOrNull()
    ?.identifier()
    ?.text
    ?.trim() == name
}

internal fun ClosureOrLambdaExpressionContext.closureContent(): BlockStatementsContext? {
  return childOfType<ClosureContext>()
    ?.childOfType<BlockStatementsOptContext>()
    ?.childOfType()
}

internal inline fun commandExpressionVisitor(
  recursive: Boolean = true,
  crossinline action: (CommandExpressionContext) -> Unit
): GroovyParserBaseVisitor<Unit> {
  return object : GroovyParserBaseVisitor<Unit>() {

    override fun visitCommandExpression(ctx: CommandExpressionContext) {
      action(ctx)

      if (recursive) {
        super.visitCommandExpression(ctx)
      }
    }
  }
}

internal inline fun pathExpressionVisitor(
  recursive: Boolean = true,
  crossinline action: (PathExpressionContext) -> Unit
): GroovyParserBaseVisitor<Unit> {
  return object : GroovyParserBaseVisitor<Unit>() {

    override fun visitPathExpression(ctx: PathExpressionContext) {
      action(ctx)

      if (recursive) {
        super.visitPathExpression(ctx)
      }
    }
  }
}

internal fun String.suppressions(): List<String> {
  return GroovyDependenciesBlockParser.NO_INSPECTION_REGEX
    .findAll(this)
    .map { it.destructured.component1() }
    .toList()
    .joinToString(",")
    .split(",")
    .map { it.trim() }
    .filter { it.isNotEmpty() }
}
