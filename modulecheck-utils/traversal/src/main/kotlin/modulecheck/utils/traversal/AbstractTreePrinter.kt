/*
 * Copyright (C) 2021-2024 Rick Busarow
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

package modulecheck.utils.traversal

import modulecheck.utils.mapLines
import modulecheck.utils.noAnsi
import modulecheck.utils.traversal.AbstractTreePrinter.Color.Companion.colorized
import modulecheck.utils.traversal.AbstractTreePrinter.NameType.SIMPLE
import modulecheck.utils.traversal.AbstractTreePrinter.NameType.TYPE

/**
 * Base class for printing a tree structure of objects of type [T].
 *
 * @param whitespaceChar the character to use for replacing
 *   whitespaces in the node text when printing. Default is ' '.
 */
abstract class AbstractTreePrinter<T : Any>(
  private val whitespaceChar: Char = ' '
) {
  private val elementSimpleNameMap = mutableMapOf<T, String>()
  private val elementTypeNameMap = mutableMapOf<T, String>()

  private var currentColorIndex = 0

  /** Returns the simple class name of an object of type [T]. */
  abstract fun T.simpleClassName(): String

  /** Returns the parent of an object of type [T]. */
  abstract fun T.parent(): T?

  /** Returns the type name of an object of type [T]. */
  abstract fun T.typeName(): String

  /** Returns the text representation of an object of type [T]. */
  abstract fun T.text(): String

  /** Returns the children of an object of type [T] as a [Sequence]. */
  abstract fun T.children(): Sequence<T>

  /**
   * Prints the tree structure of an object of type [T] to the console.
   *
   * @param [rootNode] the root node of the tree.
   */
  fun printTreeString(rootNode: T) {
    println(treeString(rootNode))
  }

  /**
   * Returns the tree structure of an object of type [T] as a string.
   *
   * @param [rootNode] the root node of the tree.
   * @return the tree structure as a string.
   */
  fun treeString(rootNode: T): String {
    return buildTreeString(rootNode, 0)
  }

  private fun buildTreeString(rootNode: T, indentLevel: Int): String {
    val indent = "╎  ".repeat(indentLevel)

    val thisName = rootNode.uniqueSimpleName()

    fun String.colorized(): String {
      return this
      // return colorized(getCurrentColor())
    }

    val parentName = rootNode.parent()?.uniqueSimpleName() ?: "null"
    val parentType = rootNode.parent()?.typeName() ?: "null"

    val childrenText = rootNode.children()
      .joinToString("\n") { child ->
        buildTreeString(child, indentLevel + 1)
      }

    val typeName = rootNode.typeName()

    @Suppress("MagicNumber")
    return buildString {

      val header =
        "$thisName [type: $typeName] [parent: $parentName] [parent type: $parentType]"

      val text = rootNode.text().replace(" ", "$whitespaceChar")

      val headerLength = header.countVisibleChars()

      val len = maxOf(headerLength + 4, text.lines().maxOf { it.countVisibleChars() })

      val headerBoxStart = "┏━".colorized()

      val headerBoxEnd = ("━".repeat(len - 3 - headerLength) + "┓").colorized()

      append("$indent$headerBoxStart $header $headerBoxEnd")

      append('\n')
      append(indent)
      append("┣${"━".repeat(len)}┫".colorized())
      append('\n')

      val paddedText = text.mapLines { line ->

        val pipe = "┃".colorized()

        "$indent$pipe${line.padEnd(len)}$pipe"
      }

      append(paddedText)

      append('\n')
      append(indent)
      append("┗${"━".repeat(len)}┛".colorized())

      if (childrenText.isNotEmpty()) {
        append("\n")
        append(childrenText)
      }
    }
  }

  private fun T.uniqueSimpleName(): String = uniqueName(SIMPLE)

  private fun T.uniqueName(nameType: NameType): String {
    val map = when (nameType) {
      SIMPLE -> elementSimpleNameMap
      TYPE -> elementTypeNameMap
    }

    return map.getOrPut(this@uniqueName) {
      val count = map.keys.count { key ->
        if (nameType == SIMPLE) {
          key.simpleClassName() == simpleClassName()
        } else {
          key.typeName() == typeName()
        }
      }

      val name = if (nameType == SIMPLE) simpleClassName() else typeName()

      val unique = if (count == 0) {
        name
      } else {
        "$name (${count + 1})"
      }

      unique.colorized(getNextColor())
    }
  }

  private fun getCurrentColor(): Color = Color.values()[currentColorIndex]

  private fun getNextColor(): Color {
    currentColorIndex = (currentColorIndex + 1) % Color.values().size
    return getCurrentColor()
  }

  private fun String.countVisibleChars(): Int = noAnsi().length

  private enum class NameType {
    SIMPLE,
    TYPE
  }

  @Suppress("MagicNumber")
  internal enum class Color(val code: Int) {
    LIGHT_RED(91),
    LIGHT_YELLOW(93),
    LIGHT_BLUE(94),
    LIGHT_GREEN(92),
    LIGHT_MAGENTA(95),
    RED(31),
    YELLOW(33),
    BLUE(34),
    GREEN(32),
    MAGENTA(35),
    CYAN(36),
    LIGHT_CYAN(96),
    ORANGE_DARK(38),
    ORANGE_BRIGHT(48),
    PURPLE_DARK(53),
    PURPLE_BRIGHT(93),
    PINK_BRIGHT(198),
    BROWN_DARK(94),
    BROWN_BRIGHT(178),
    LIGHT_GRAY(37),
    DARK_GRAY(90),
    BLACK(30),
    WHITE(97);

    companion object {

      private val supported = "win" !in System.getProperty("os.name").lowercase()

      /** returns a string in the given color */
      fun String.colorized(color: Color): String {

        return if (supported) {
          "\u001B[${color.code}m$this\u001B[0m"
        } else {
          this
        }
      }
    }
  }
}
