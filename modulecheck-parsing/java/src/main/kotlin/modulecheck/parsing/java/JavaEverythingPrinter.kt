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

package modulecheck.parsing.java

import com.github.javaparser.ast.Node
import modulecheck.utils.remove

internal class JavaEverythingPrinter {

  private val parentNameMap = mutableMapOf<Node, String>()

  fun visit(node: Node) {

    val thisName = node::class.java.simpleName
    val parentName = node.parentName()

    println(
      """ ******************************** -- $thisName  -- parent: $parentName
      |$node
      |_________________________________________________________________________________
      """.trimMargin()
    )

    node.childNodes.forEach { child ->
      visit(child)
    }
  }

  private fun Node.parentName() = parentNode.getOrNull()
    ?.let { parent ->

      parentNameMap.getOrPut(parent) {
        val typeCount = parentNameMap.keys.count { it::class == parent::class }

        val simpleName = parent::class.java.simpleName

        val start = if (typeCount == 0) {
          simpleName
        } else {
          "$simpleName (${typeCount + 1})"
        }

        start // + parent.extendedTypes()
      }
    }

  @Suppress("UnusedPrivateMember")
  private fun Node.extendedTypes(): String {
    return this::class.supertypes
      .ifEmpty { return "" }
      .joinToString(prefix = " [ ", postfix = " ] ") { kType ->
        kType.toString()
          .remove("com.github.javaparser.ast.body.")
      }
  }
}

internal fun Node.printEverything() {
  JavaEverythingPrinter().visit(this)
}
