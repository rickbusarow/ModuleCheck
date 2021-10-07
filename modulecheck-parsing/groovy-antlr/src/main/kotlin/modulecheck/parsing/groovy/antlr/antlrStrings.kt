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

private val multiLineCommentRegex = "\\s*\\/\\*[^\\*]*?\\*\\/\\s*".toRegex()
private val lineWithoutCommentRegex = "^(?!.*\\/\\/).*\$".toRegex()
private val lineWithCommentRegex = "([\\s\\S]*)(\\/\\/.*)".toRegex()

/**
 * Antlr doesn't handle block comments (`/* block comment */`) well, so this will:
 * 1. strip all whitespaces from inside the comment (Antlr would do this anyway)
 * 2. ensure a new-line before the comment (Antlr will just delete a trailing block comment)
 * 3. ensure a new-line after the comment (Antlr will just delete a leading block comment)
 */
fun String.collapseBlockComments() = replace(multiLineCommentRegex) { mr ->
  "\n" + mr.value.replace("\\s*".toRegex(), "") + "\n"
}

/**
 * Removes any whitespaces from the start of each line.  Antlr does this anyway.
 */
fun String.trimEachLineStart() = lines()
  .joinToString("\n") { line ->
    line.trimStart()
  }

/**
 * Removes all whitespaces from the [String] receiver just as Antlr's Groovy parser does.
 *
 * Anything following single-line comment markers ("//________") will be unaltered.
 *
 * Anything else will have **all** spacing removed.
 */
fun String.trimLinesLikeAntlr() = lines()
  .joinToString("\n") { line ->
    line
      .replace(lineWithoutCommentRegex) { mr ->
        mr.value.replace("\\s".toRegex(), "")
      }
      .replace(lineWithCommentRegex) { mr ->

        val beforeComment = mr.groups[1]?.value ?: mr.value
        val comment = mr.groups[2]?.value ?: ""

        beforeComment.replace("\\s".toRegex(), "") + comment
      }
  }
