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

package modulecheck.utils

import java.util.Locale

fun String.prefixIfNot(prefix: String) =
  if (this.startsWith(prefix)) this else "$prefix$this"

fun String.suffixIfNot(suffix: String) =
  if (this.endsWith(suffix)) this else "$this$suffix"

fun String.decapitalize(
  locale: Locale = Locale.US
) = replaceFirstChar { it.lowercase(locale) }

fun String.capitalize(
  locale: Locale = Locale.US
) = replaceFirstChar { it.uppercase(locale) }

fun String.findMinimumIndent(
  absoluteMinimum: String = "  "
): String {

  if (contains("\t")) return "\t"

  val parsedMinimumOrNull = lines()
    .filter { it.isNotBlank() }
    .map { it.indentWidth() }
    .filter { it > 0 }
    .minOrNull()
    ?.let { " ".repeat(it) }

  return parsedMinimumOrNull
    ?.takeIf { it.length >= absoluteMinimum.length }
    ?: absoluteMinimum
}

private fun String.indentWidth(): Int =
  indexOfFirst { !it.isWhitespace() }.let { if (it == -1) length else it }

/**
 * A builder scope for [StringBuilder.indent][modulecheck.utils.indent] and [buildStringIndented].
 *
 * @since 0.13.0
 */
class IndentScope(
  @PublishedApi
  internal val indent: String,
  @PublishedApi
  internal val tab: String,
  @PublishedApi
  internal val stringBuilder: StringBuilder
) {

  fun append(str: String) {
    stringBuilder.append(indent + str)
  }

  fun appendLine(str: String) {
    stringBuilder.appendLine(indent + str)
  }

  fun append(c: Char) {
    stringBuilder.append(indent + c)
  }

  fun appendLine(c: Char) {
    stringBuilder.appendLine(indent + c)
  }

  /**
   * Creates another layer of indentation by adding [tab] to [indent], then performing [action].
   *
   * @since 0.13.0
   */
  inline fun indent(
    tab: String = this.tab,
    action: IndentScope.() -> Unit
  ) {
    IndentScope(
      indent = indent + tab,
      tab = tab,
      stringBuilder = stringBuilder
    )
      .action()
  }
}

/**
 * Shorthand version of [StringBuilder.indent][modulecheck.utils.indent] for when the first line of
 * the `buildString { ... }` block would just be a call to `indent(...) { ... }`.
 *
 * example:
 * ```
 * override fun toString() = buildStringIndented(baseIndent = "      ") {
 *   appendLine("SomeClass(")
 *   indent {
 *     appendLine("prop1=$prop1")
 *     appendLine("prop2=$prop2")
 *   }
 *   eppendLine(")")
 * }
 * ```
 *
 * @since 0.13.0
 */
inline fun buildStringIndented(
  baseIndent: String,
  tab: String = "  ",
  action: IndentScope.() -> Unit
): String {
  return buildString {
    indent(
      startingIndent = baseIndent,
      tab = tab,
      action = action
    )
  }
}

/**
 * example:
 * ```
 * override fun toString() = buildString {
 *   appendLine("SomeClass(")
 *   indent {
 *     appendLine("prop1=$prop1")
 *     appendLine("prop2=$prop2")
 *   }
 *   eppendLine(")")
 * }
 * ```
 *
 * @since 0.13.0
 */
inline fun StringBuilder.indent(
  startingIndent: String = "",
  tab: String = "  ",
  action: IndentScope.() -> Unit
) {
  IndentScope(
    indent = startingIndent,
    tab = tab,
    stringBuilder = this
  ).action()
}

/**
 * A naive auto-indent which just counts brackets.
 *
 * @since 0.12.0
 */
fun String.indentByBrackets(tab: String = "  "): String {

  var tabCount = 0

  val open = setOf('{', '(', '[', '<')
  val close = setOf('}', ')', ']', '>')

  return lines()
    .map { it.trim() }
    .joinToString("\n") { line ->

      if (line.firstOrNull() in close) {
        tabCount--
      }

      "${tab.repeat(tabCount)}$line"
        .also {

          // Arrows aren't brackets
          val noSpecials = line.remove("<=", "->")

          tabCount += noSpecials.count { char -> char in open }
          // Skip the first char because if it's a closing bracket, it was already counted above.
          tabCount -= noSpecials.drop(1).count { char -> char in close }
        }
    }
}

fun String.remove(vararg strings: String): String = strings.fold(this) { acc, string ->
  acc.replace(string, "")
}

fun String.remove(vararg patterns: Regex): String = patterns.fold(this) { acc, regex ->
  acc.replace(regex, "")
}

/**
 * @return a string with no leading or trailing whitespace, and no whitespace before or after any
 *     instance of [delimiter]
 * @since 0.12.0
 */
fun String.trimSegments(delimiter: String = "."): String {
  val escapedDelimiter = Regex.escape(delimiter)
  return replaceRegex(
    regex = """(?>\s+$escapedDelimiter\s+)|(?>\s+$escapedDelimiter)|(?>$escapedDelimiter\s+)""",
    replacement = delimiter
  )
    .trim()
}
