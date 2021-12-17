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

import groovyjarjarantlr4.v4.runtime.CharStream
import groovyjarjarantlr4.v4.runtime.ParserRuleContext
import groovyjarjarantlr4.v4.runtime.RuleContext
import groovyjarjarantlr4.v4.runtime.misc.Interval
import org.apache.groovy.parser.antlr4.GroovyParser.AssignmentExprAltContext
import org.apache.groovy.parser.antlr4.GroovyParser.BlockStatementsContext
import org.apache.groovy.parser.antlr4.GroovyParser.BlockStatementsOptContext
import org.apache.groovy.parser.antlr4.GroovyParser.ClosureContext
import org.apache.groovy.parser.antlr4.GroovyParser.ClosureOrLambdaExpressionContext
import org.apache.groovy.parser.antlr4.GroovyParser.CommandExpressionContext
import org.apache.groovy.parser.antlr4.GroovyParser.IdentifierPrmrAltContext
import org.apache.groovy.parser.antlr4.GroovyParser.NamePartContext
import org.apache.groovy.parser.antlr4.GroovyParser.PathElementContext
import org.apache.groovy.parser.antlr4.GroovyParser.PathExpressionContext
import org.apache.groovy.parser.antlr4.GroovyParser.PostfixExprAltContext
import org.apache.groovy.parser.antlr4.GroovyParser.PostfixExpressionContext
import org.apache.groovy.parser.antlr4.GroovyParserBaseVisitor

internal inline fun <reified T> RuleContext.parentOfType(): T? {
  return generateSequence(this as? RuleContext?) { it.parent }
    .filterIsInstance<T>()
    .firstOrNull()
}

internal inline fun <reified T> ParserRuleContext.childrenOfTypeRecursive(): List<T> {
  return generateSequence(sequenceOf(this)) { parserRuleContexts ->
    parserRuleContexts.mapNotNull { it.children }
      .flatten()
      .filterIsInstance<ParserRuleContext>()
      .takeIf { it.firstOrNull() != null }
  }
    .flatten()
    .filterIsInstance<T>()
    .toList()
}

internal inline fun <reified T> ParserRuleContext.childrenOfType(): List<T> {
  return children.filterIsInstance<T>()
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
  return stream.getText(Interval(start.startIndex, stop.stopIndex))
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
