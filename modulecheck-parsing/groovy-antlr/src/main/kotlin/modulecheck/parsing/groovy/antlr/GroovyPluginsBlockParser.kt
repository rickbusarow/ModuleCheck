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

import groovyjarjarantlr4.v4.runtime.CharStreams
import groovyjarjarantlr4.v4.runtime.CommonTokenStream
import org.apache.groovy.parser.antlr4.GroovyLangLexer
import org.apache.groovy.parser.antlr4.GroovyLangParser
import org.apache.groovy.parser.antlr4.GroovyParser
import org.apache.groovy.parser.antlr4.GroovyParserBaseVisitor

class GroovyPluginsBlockParser {

  fun parse(file: String): GroovyPluginsBlock? {

    val stream = CharStreams.fromString(file)

    val lexer = GroovyLangLexer(stream)
    val tokens = CommonTokenStream(lexer)

    var block: GroovyPluginsBlock? = null

    val visitor = object : GroovyParserBaseVisitor<Unit>() {

      override fun visitScriptStatement(ctx: GroovyParser.ScriptStatementContext?) {
        super.visitScriptStatement(ctx)

        val statement = ctx?.statement()

        if (statement?.start?.text == "plugins") {
          val blockBodyReg = """plugins\s*\{([\s\S]*)\}""".toRegex()

          val blockBody = blockBodyReg.find(file)
            ?.groupValues
            ?.get(1)
            ?.removePrefix("\n")
            ?: return

          val pluginsBlock = GroovyPluginsBlock(blockBody)

          val blockStatementVisitor = object : GroovyParserBaseVisitor<Unit>() {

            override fun visitBlockStatement(ctx: GroovyParser.BlockStatementContext) {
              super.visitBlockStatement(ctx)

              pluginsBlock.addStatement(
                parsedString = ctx.originalText(stream)
              )
            }
          }

          blockStatementVisitor.visit(ctx)

          block = pluginsBlock
        }
      }
    }

    GroovyLangParser(tokens)
      .compilationUnit()
      .accept(visitor)

    return block
  }
}
