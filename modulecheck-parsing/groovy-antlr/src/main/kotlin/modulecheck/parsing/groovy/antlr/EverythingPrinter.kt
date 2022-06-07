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

import groovyjarjarantlr4.v4.runtime.ParserRuleContext
import groovyjarjarantlr4.v4.runtime.RuleContext
import modulecheck.utils.requireNotNull

internal fun ParserRuleContext.printEverything() {

  val levels = mutableMapOf<RuleContext, Int>(this to 0)
  val dashes = "------------------------------------------------------------"

  fun printNode(nodeSimpleName: String, parentName: String, nodeText: String, level: Int) {
    println(
      """
      |   $dashes  $nodeSimpleName    -- parent: $parentName
      |
      |   `$nodeText`
      """.trimMargin()
        .lines()
        .let {
          it.dropLast(1) + it.last().replaceFirst("  ", "└─")
        }
        .joinToString("\n")
        .prependIndent("│   ".repeat(level))
    )
  }

  printNode(
    nodeSimpleName = javaClass.simpleName, parentName = "null", nodeText = text, level = 0
  )

  childrenOfTypeRecursive<ParserRuleContext>()
    .filterNot { it == this }
    .forEach { node ->

      val parent = node.parent.requireNotNull {
        "Parent is null for ${node.javaClass.simpleName}, but that's impossible?"
      }

      val parentLevel = levels.getValue(parent)
      levels[node] = parentLevel + 1

      val parentName = parent.javaClass.simpleName

      printNode(
        nodeSimpleName = node.javaClass.simpleName,
        parentName = parentName,
        nodeText = node.text,
        level = parentLevel + 1
      )
    }
}
