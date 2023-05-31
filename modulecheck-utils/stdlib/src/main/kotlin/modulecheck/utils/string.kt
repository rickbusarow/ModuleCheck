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

package modulecheck.utils

import java.util.Locale

/**
 * Appends the specified [prefix] to this [CharSequence] and returns the resulting string.
 *
 * @param prefix The prefix to be added.
 * @receiver The original CharSequence.
 * @return The string with the prefix added.
 */
fun CharSequence.prefix(prefix: String): String = "$prefix$this"

/**
 * Adds the specified [prefix] to this [String] if it does not already start with it.
 *
 * @param prefix The prefix to be added if not present.
 * @receiver The original String.
 * @return The string with the prefix added if not present.
 */
fun String.prefixIfNot(prefix: String): String =
  if (this.startsWith(prefix)) this else "$prefix$this"

/**
 * Adds the specified [suffix] to this [String] if it does not already end with it.
 *
 * @param suffix The suffix to be added if not present.
 * @receiver The original String.
 * @return The string with the suffix added if not present.
 */
fun String.suffixIfNot(suffix: String): String = if (this.endsWith(suffix)) this else "$this$suffix"

/**
 * Decapitalizes the first character of this [String] using the specified [locale].
 *
 * @param locale The [Locale] to be used for decapitalization. Defaults to [Locale.US].
 * @receiver The original String.
 * @return The string with the first character decapitalized.
 */
fun String.decapitalize(locale: Locale = Locale.US): String =
  replaceFirstChar { it.lowercase(locale) }

/**
 * Capitalizes the first character of this [String] using the specified [locale].
 *
 * @param locale The [Locale] to be used for capitalization. Defaults to [Locale.US].
 * @receiver The original String.
 * @return The string with the first character capitalized.
 */
fun String.capitalize(locale: Locale = Locale.US): String =
  replaceFirstChar { it.uppercase(locale) }

/**
 * Finds and returns the minimum indentation for this [String], with an optional
 * [absoluteMinimum] to be returned if the minimum indent is less than the absolute minimum.
 *
 * @param absoluteMinimum The absolute minimum indent to
 *   be returned if the minimum is less. Defaults to " ".
 * @receiver The original String.
 * @return The minimum indentation of the string, or the absolute minimum if the minimum is less.
 */
fun String.findMinimumIndent(absoluteMinimum: String = "  "): String {

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

/**
 * Calculates and returns the width of the indentation for this [String].
 *
 * @receiver The original String.
 * @return The width of the indentation for this string.
 */
private fun String.indentWidth(): Int =
  indexOfFirst { !it.isWhitespace() }.let { if (it == -1) length else it }

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
 * @return a string with no leading or trailing whitespace, and
 *   no whitespace before or after any instance of [delimiter]
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

/**
 * performs [transform] on each line
 *
 * Doesn't preserve the original line endings.
 */
fun CharSequence.mapLines(transform: (String) -> CharSequence): String = lineSequence()
  .joinToString("\n", transform = transform)

/**
 * performs [transform] on each line
 *
 * Doesn't preserve the original line endings.
 */
fun CharSequence.mapLinesIndexed(transform: (Int, String) -> CharSequence): String = lineSequence()
  .mapIndexed(transform)
  .joinToString("\n")

/**
 * Creates a string from all the elements separated using [separator]
 * and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value
 * of [limit], in which case only the first [limit] elements will be
 * appended, followed by the [truncated] string (which defaults to "...").
 *
 * The operation is _terminal_.
 */
@Suppress("LongParameterList")
fun <T> Sequence<T>.joinToStringIndexed(
  separator: CharSequence = ", ",
  prefix: CharSequence = "",
  postfix: CharSequence = "",
  limit: Int = -1,
  truncated: CharSequence = "...",
  transform: (Int, T) -> CharSequence
): String {
  return buildString {
    append(prefix)
    var count = 0
    for (element in this@joinToStringIndexed) {
      if (++count > 1) append(separator)
      if (limit < 0 || count <= limit) {
        append(transform(count - 1, element))
      } else {
        break
      }
    }
    if (limit in 0 until count) append(truncated)
    append(postfix)
  }
}

/**
 * example:
 *
 * ```
 * override fun toString() = buildString {
 *   appendLine("SomeClass(")
 *   indent {
 *     appendLine("prop1=$prop1")
 *     appendLine("prop2=$prop2")
 *   }
 *   appendLine(")")
 * }
 * ```
 */
inline fun StringBuilder.indent(
  leadingIndent: String = "  ",
  continuationIndent: String = leadingIndent,
  builder: StringBuilder.() -> Unit
) {

  append(
    buildString {
      append(leadingIndent)

      builder()
    }
      .prependContinuationIndent(continuationIndent)
  )
}

/**
 * Prepends [continuationIndent] to every line of the original string.
 *
 * Doesn't preserve the original line endings.
 */
fun CharSequence.prependContinuationIndent(
  continuationIndent: String,
  skipBlankLines: Boolean = true
): String = mapLinesIndexed { i, line ->
  when {
    i == 0 -> line
    skipBlankLines && line.isBlank() -> line
    else -> "$continuationIndent$line"
  }
}

/** `"$prefix$this$suffix"` */
fun CharSequence.wrapIn(prefix: String, suffix: String = prefix): String = "$prefix$this$suffix"
