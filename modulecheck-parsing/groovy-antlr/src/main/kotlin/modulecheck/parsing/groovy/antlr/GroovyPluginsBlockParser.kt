/*
 * Copyright (C) 2021-2022 Rick Busarow
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

import modulecheck.reporting.logging.McLogger
import org.apache.groovy.parser.antlr4.GroovyParser.BlockStatementContext
import org.apache.groovy.parser.antlr4.GroovyParser.NlsContext
import org.apache.groovy.parser.antlr4.GroovyParser.ScriptStatementContext
import org.apache.groovy.parser.antlr4.GroovyParser.SepContext
import org.apache.groovy.parser.antlr4.GroovyParserBaseVisitor
import java.io.File
import javax.inject.Inject

class GroovyPluginsBlockParser @Inject constructor(
  private val logger: McLogger
) {

  fun parse(file: File): GroovyPluginsBlock? = parse(file) {

    var block: GroovyPluginsBlock? = null

    val visitor = object : GroovyParserBaseVisitor<Unit>() {

      var pendingBlockNoInspectionComments = mutableListOf<String>()

      override fun visitNls(ctx: NlsContext) {
        super.visitNls(ctx)

        pendingBlockNoInspectionComments.addAll(
          GroovyDependenciesBlockParser.NO_INSPECTION_REGEX
            .findAll(ctx.text)
            .map { it.destructured.component1() }
        )
      }

      override fun visitScriptStatement(ctx: ScriptStatementContext?) {
        super.visitScriptStatement(ctx)

        val statement = ctx?.statement()

        if (statement?.start?.text == "plugins") {
          val blockBodyReg = """plugins\s*\{([\s\S]*)\}""".toRegex()

          val blockBody = blockBodyReg.find(file.readText())
            ?.groupValues
            ?.get(1)
            ?.removePrefix("\n")
            ?: return

          val blockSuppressed = pendingBlockNoInspectionComments.joinToString(",")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
          pendingBlockNoInspectionComments.clear()

          val pluginsBlock = GroovyPluginsBlock(
            logger = logger,
            fullText = statement.originalText(),
            lambdaContent = blockBody,
            suppressedForEntireBlock = blockSuppressed
          )

          val blockStatementVisitor = object : GroovyParserBaseVisitor<Unit>() {

            var pendingNoInspectionComments = mutableListOf<String>()

            override fun visitSep(ctx: SepContext) {
              super.visitSep(ctx)

              pendingNoInspectionComments.addAll(
                GroovyDependenciesBlockParser.NO_INSPECTION_REGEX
                  .findAll(ctx.text)
                  .map { it.destructured.component1() }
              )
            }

            override fun visitBlockStatement(ctx: BlockStatementContext) {
              super.visitBlockStatement(ctx)

              val suppressed = pendingNoInspectionComments.joinToString(",")
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
              pendingNoInspectionComments.clear()

              pluginsBlock.addStatement(
                parsedString = ctx.originalText(),
                suppressed = suppressed
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
