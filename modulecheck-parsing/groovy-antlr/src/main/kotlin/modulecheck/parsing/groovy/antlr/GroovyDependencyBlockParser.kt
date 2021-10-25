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

@file:Suppress("RegExpRedundantEscape")

package modulecheck.parsing.groovy.antlr

import groovyjarjarantlr4.v4.runtime.*
import groovyjarjarantlr4.v4.runtime.misc.Interval
import groovyjarjarantlr4.v4.runtime.tree.RuleNode
import modulecheck.parsing.MavenCoordinates
import modulecheck.parsing.ModuleRef
import modulecheck.parsing.asConfigurationName
import org.apache.groovy.parser.antlr4.GroovyLangLexer
import org.apache.groovy.parser.antlr4.GroovyLangParser
import org.apache.groovy.parser.antlr4.GroovyParser.BlockStatementContext
import org.apache.groovy.parser.antlr4.GroovyParser.ExpressionListElementContext
import org.apache.groovy.parser.antlr4.GroovyParser.ScriptStatementContext
import org.apache.groovy.parser.antlr4.GroovyParser.StringLiteralContext
import org.apache.groovy.parser.antlr4.GroovyParserBaseVisitor

class GroovyDependencyBlockParser {

  fun parse(file: String): List<GroovyDependenciesBlock> {
    val dependenciesBlocks = mutableListOf<GroovyDependenciesBlock>()

    val stream = CharStreams.fromString(file)

    val lexer = GroovyLangLexer(stream)
    val tokens = CommonTokenStream(lexer)

    val rawModuleNameVisitor = object : GroovyParserBaseVisitor<String?>() {

      override fun shouldVisitNextChild(
        node: RuleNode?,
        currentResult: String?
      ): Boolean = currentResult == null

      override fun visitStringLiteral(ctx: StringLiteralContext?): String? {
        return ctx?.originalText(stream)?.replace("""["']""".toRegex(), "")
      }
    }

    val projectDepVisitor = object : GroovyParserBaseVisitor<String?>() {

      override fun shouldVisitNextChild(
        node: RuleNode?,
        currentResult: String?
      ): Boolean = currentResult == null

      override fun visitExpressionListElement(ctx: ExpressionListElementContext?): String? {
        return visitChildren(ctx) ?: when (ctx?.start?.text) {
          "projects" -> {
            ctx.originalText(stream).removePrefix("projects.")
          }
          "project" -> {
            ctx.accept(rawModuleNameVisitor)
          }
          else -> {
            null
          }
        }
      }
    }

    val unknownArgumentVisitor = object : GroovyParserBaseVisitor<String?>() {

      override fun shouldVisitNextChild(
        node: RuleNode?,
        currentResult: String?
      ): Boolean = currentResult == null

      override fun visitExpressionListElement(ctx: ExpressionListElementContext): String {
        return ctx.originalText(stream)
      }
    }

    val visitor = object : GroovyParserBaseVisitor<Unit>() {

      override fun visitScriptStatement(ctx: ScriptStatementContext?) {

        val statement = ctx?.statement()

        if (statement?.start?.text == "dependencies") {

          super.visitScriptStatement(ctx)

          val originalBlockBody = statement.parentOfType<ScriptStatementContext>()
            ?.originalText(stream)
            ?: return

          val blockBody = BLOCK_BODY_REGEX.find(originalBlockBody)
            ?.groupValues
            ?.get(1)
            ?.removePrefix("\n")
            ?: return

          val dependenciesBlock = GroovyDependenciesBlock(blockBody)

          val blockStatementVisitor = object : GroovyParserBaseVisitor<Unit>() {

            override fun visitBlockStatement(ctx: BlockStatementContext) {

              val config = ctx.start.text

              val moduleRefString = projectDepVisitor.visit(ctx)

              if (moduleRefString != null) {
                dependenciesBlock.addModuleStatement(
                  configName = config.asConfigurationName(),
                  parsedString = ctx.originalText(stream),
                  moduleRef = ModuleRef.from(moduleRefString)
                )
                return
              }

              val mavenCoordinates = rawModuleNameVisitor.visit(ctx)
                ?.let { MavenCoordinates.parseOrNull(it) }

              if (mavenCoordinates != null) {
                dependenciesBlock.addNonModuleStatement(
                  configName = config.asConfigurationName(),
                  parsedString = ctx.originalText(stream),
                  coordinates = mavenCoordinates
                )
                return
              }

              val argument = unknownArgumentVisitor.visit(ctx) ?: return

              dependenciesBlock.addUnknownStatement(
                configName = config.asConfigurationName(),
                parsedString = ctx.originalText(stream),
                argument = argument
              )
            }
          }

          blockStatementVisitor.visit(ctx)

          dependenciesBlocks.add(dependenciesBlock)
        }
      }
    }

    GroovyLangParser(tokens)
      .compilationUnit()
      .accept(visitor)

    return dependenciesBlocks
  }

  companion object {
    val BLOCK_BODY_REGEX = """dependencies\s*\{([\s\S]*)\}""".toRegex()
  }
}

inline fun <reified T> RuleContext.parentOfType(): T? {
  return generateSequence(this as? RuleContext?) { it.parent }
    .filterIsInstance<T>()
    .firstOrNull()
}

fun ParserRuleContext.originalText(stream: CharStream): String {
  return stream.getText(Interval(start.startIndex, stop.stopIndex))
}

inline fun blockStatementVisitor(
  crossinline action: (BlockStatementContext) -> Unit
): GroovyParserBaseVisitor<Unit> {
  return object : GroovyParserBaseVisitor<Unit>() {
    override fun visitBlockStatement(ctx: BlockStatementContext) {
      action(ctx)
    }
  }
}
