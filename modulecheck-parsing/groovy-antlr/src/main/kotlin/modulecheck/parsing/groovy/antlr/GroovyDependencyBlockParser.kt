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

import groovyjarjarantlr4.v4.runtime.CharStreams
import groovyjarjarantlr4.v4.runtime.CommonTokenStream
import groovyjarjarantlr4.v4.runtime.tree.RuleNode
import modulecheck.parsing.MavenCoordinates
import org.apache.groovy.parser.antlr4.GroovyLangLexer
import org.apache.groovy.parser.antlr4.GroovyLangParser
import org.apache.groovy.parser.antlr4.GroovyParser.BlockStatementContext
import org.apache.groovy.parser.antlr4.GroovyParser.PostfixExpressionContext
import org.apache.groovy.parser.antlr4.GroovyParser.ScriptStatementContext
import org.apache.groovy.parser.antlr4.GroovyParser.StringLiteralContext
import org.apache.groovy.parser.antlr4.GroovyParserBaseVisitor

class GroovyDependencyBlockParser {

  fun parse(file: String): List<GroovyDependenciesBlock> {
    val dependenciesBlocks = mutableListOf<GroovyDependenciesBlock>()

    val flattened = file.collapseBlockComments()
      .trimEachLineStart()

    val lexer = GroovyLangLexer(CharStreams.fromString(flattened))
    val tokens = CommonTokenStream(lexer)

    val rawModuleNameVisitor = object : GroovyParserBaseVisitor<String?>() {

      override fun shouldVisitNextChild(
        node: RuleNode?,
        currentResult: String?
      ): Boolean = currentResult == null

      override fun visitStringLiteral(ctx: StringLiteralContext?): String? {
        return ctx?.text?.replace("""["']""".toRegex(), "")
      }
    }

    val projectDepVisitor = object : GroovyParserBaseVisitor<String?>() {

      override fun shouldVisitNextChild(
        node: RuleNode?,
        currentResult: String?
      ): Boolean = currentResult == null

      override fun visitPostfixExpression(ctx: PostfixExpressionContext?): String? {
        return visitChildren(ctx) ?: when (ctx?.start?.text) {
          "projects" -> {
            ctx.text.removePrefix("projects.")
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

    val visitor = object : GroovyParserBaseVisitor<Unit>() {

      override fun visitScriptStatement(ctx: ScriptStatementContext?) {
        super.visitScriptStatement(ctx)

        val statement = ctx?.statement()

        if (statement?.start?.text == "dependencies") {
          val blockBodyReg = """dependencies\s*\{([\s\S]*)\}""".toRegex()

          val blockBody = blockBodyReg.find(file)
            ?.groupValues
            ?.get(1)
            ?.removePrefix("\n")
            ?: return

          val dependenciesBlock = GroovyDependenciesBlock(blockBody)

          val blockStatementVisitor = object : GroovyParserBaseVisitor<Unit>() {

            override fun visitBlockStatement(ctx: BlockStatementContext) {
              super.visitBlockStatement(ctx)

              val config = ctx.start.text

              val moduleRef = projectDepVisitor.visit(ctx)

              if (moduleRef != null) {
                dependenciesBlock.addModuleStatement(
                  moduleRef = moduleRef,
                  configName = config,
                  parsedString = ctx.text
                )
                return
              }

              val mavenCoordinates = rawModuleNameVisitor.visit(ctx)
                ?.let { MavenCoordinates.parseOrNull(it) }

              if (mavenCoordinates != null) {
                dependenciesBlock.addNonModuleStatement(
                  configName = config,
                  parsedString = ctx.text,
                  coordinates = mavenCoordinates
                )
                return
              }

              dependenciesBlock.addUnknownStatement(config, ctx.text)
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
}
