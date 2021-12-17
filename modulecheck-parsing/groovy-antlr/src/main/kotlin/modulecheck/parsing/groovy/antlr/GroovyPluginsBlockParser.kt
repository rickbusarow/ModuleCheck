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

import org.apache.groovy.parser.antlr4.GroovyParser.BlockStatementContext
import org.apache.groovy.parser.antlr4.GroovyParser.ScriptStatementContext
import org.apache.groovy.parser.antlr4.GroovyParserBaseVisitor
import javax.inject.Inject

class GroovyPluginsBlockParser @Inject constructor() {

  fun parse(file: String): GroovyPluginsBlock? = parse(file) {

    var block: GroovyPluginsBlock? = null

    val visitor = object : GroovyParserBaseVisitor<Unit>() {

      override fun visitScriptStatement(ctx: ScriptStatementContext?) {
        super.visitScriptStatement(ctx)

        val statement = ctx?.statement()

        if (statement?.start?.text == "plugins") {
          val blockBodyReg = """plugins\s*\{([\s\S]*)\}""".toRegex()

          val blockBody = blockBodyReg.find(file)
            ?.groupValues
            ?.get(1)
            ?.removePrefix("\n")
            ?: return

          val pluginsBlock = GroovyPluginsBlock(
            fullText = statement.originalText(),
            contentString = blockBody
          )

          val blockStatementVisitor = object : GroovyParserBaseVisitor<Unit>() {

            override fun visitBlockStatement(ctx: BlockStatementContext) {
              super.visitBlockStatement(ctx)

              pluginsBlock.addStatement(
                parsedString = ctx.originalText()
              )
            }
          }

          blockStatementVisitor.visit(ctx)

          block = pluginsBlock
        }
      }
    }

    parser.accept(visitor)

    return block
  }
}
