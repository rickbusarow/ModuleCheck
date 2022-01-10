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

fun String.decapitalize(
  locale: Locale = Locale.US
) = replaceFirstChar { it.lowercase(locale) }

fun String.capitalize(
  locale: Locale = Locale.US
) = replaceFirstChar { it.uppercase(locale) }

fun String.minimumIndent(
  absoluteMinimum: String = "  "
): String {

  if (contains("\t")) return "\t"

  return lines()
    .map { it.indentWidth() }
    .filter { it > 0 }
    .minOrNull()
    ?.let { " ".repeat(it) }
    ?: absoluteMinimum
}

private fun String.indentWidth(): Int =
  indexOfFirst { !it.isWhitespace() }.let { if (it == -1) length else it }

class IndentScope(private val indent: String, private val stringBuilder: StringBuilder) {

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

  fun indent(indent: String, action: IndentScope.() -> Unit) {
    IndentScope(this.indent + indent, stringBuilder).action()
  }
}

fun StringBuilder.indent(indent: String, action: IndentScope.() -> Unit) {
  IndentScope(indent, this).action()
}

fun String.remove(vararg strings: String): String = strings.fold(this) { acc, string ->
  acc.replace(string, "")
}

fun String.remove(vararg patterns: Regex): String = patterns.fold(this) { acc, regex ->
  acc.replace(regex, "")
}

inline fun <T, R> Collection<T>.mapToSet(transform: (T) -> R): Set<R> {
  return mapTo(mutableSetOf<R>(), transform)
}
