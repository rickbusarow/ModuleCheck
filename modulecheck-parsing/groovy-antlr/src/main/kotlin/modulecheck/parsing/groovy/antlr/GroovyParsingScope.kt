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
import groovyjarjarantlr4.v4.runtime.CodePointCharStream
import groovyjarjarantlr4.v4.runtime.CommonTokenStream
import groovyjarjarantlr4.v4.runtime.ParserRuleContext
import org.apache.groovy.parser.antlr4.GroovyLangLexer
import org.apache.groovy.parser.antlr4.GroovyLangParser
import org.apache.groovy.parser.antlr4.GroovyParser.CompilationUnitContext
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
internal data class GroovyParsingScope(val fullText: String) {
  val stream: CodePointCharStream = CharStreams.fromString(fullText)
  val lexer = GroovyLangLexer(stream)
  val tokens = CommonTokenStream(lexer)

  val parser: CompilationUnitContext = GroovyLangParser(tokens).compilationUnit()

  fun ParserRuleContext.originalText(): String {
    return originalText(stream)
  }
}

internal inline fun <T> parse(file: File, parsingAction: GroovyParsingScope.() -> T): T {
  return GroovyParsingScope(file.readText()).parsingAction()
}

internal inline fun <T> parse(fullText: String, parsingAction: GroovyParsingScope.() -> T): T {
  return GroovyParsingScope(fullText).parsingAction()
}
