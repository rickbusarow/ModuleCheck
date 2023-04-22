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

package modulecheck.utils.traversal

/** Depth-first and breadth-first traversals of any tree */
object Traversals {

  /**
   * Generates a depth-first traversal sequence for a given tree or graph structure.
   *
   * @param t The root element of the tree/graph.
   * @param childrenFactory A lambda that returns the children of a given element.
   * @return A depth-first traversal sequence for the tree/graph.
   */
  inline fun <T : Any> depthFirstTraversal(
    t: T,
    crossinline childrenFactory: T.() -> List<T>
  ): Sequence<T> {
    val stack = ArrayDeque<T>()
    stack.addLast(t)

    return generateSequence {
      stack.removeLastOrNull()
        ?.also { current ->
          val children = childrenFactory(current)
          when (children.size) {
            0 -> Unit // Do nothing for empty children
            1 -> stack.addLast(children[0]) // Add the only child directly
            else -> stack.addAll(children.asReversed()) // Add all the children in reversed order
          }
        }
    }
  }

  /**
   * Generates a breadth-first traversal sequence for a given tree or graph structure.
   *
   * @param t The root element of the tree/graph.
   * @param childrenFactory A lambda that returns the children of a given element.
   * @return A breadth-first traversal sequence for the tree/graph.
   */
  inline fun <T : Any> breadthFirstTraversal(
    t: T,
    crossinline childrenFactory: T.() -> List<T>
  ): Sequence<T> {
    val queue = ArrayDeque<T>()
    queue.addLast(t)

    return generateSequence {
      val current = queue.removeFirstOrNull() ?: return@generateSequence null
      val children = childrenFactory(current)

      when (children.size) {
        // 0 -> Unit // Do nothing for empty children
        // 1 -> queue.addLast(children[0]) // Add the only child directly
        else -> queue.addAll(children)
      }

      current
    }
  }
}
