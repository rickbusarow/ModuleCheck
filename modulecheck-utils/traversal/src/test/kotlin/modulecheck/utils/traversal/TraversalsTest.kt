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

import io.kotest.matchers.collections.shouldContainExactly
import modulecheck.utils.traversal.Traversals.breadthFirstTraversal
import modulecheck.utils.traversal.Traversals.depthFirstTraversal
import org.junit.jupiter.api.Test

class TraversalsTest {
  @Test
  fun `depthFirstTraversal should return nodes in the correct order`() {
    val tree = rootNode("root", "rootType") {
      compositeNode("a", "typeA") {
        leafNode("a1", "typeA1")
        leafNode("a2", "typeA2")
      }
      compositeNode("b", "typeB") {
        leafNode("b1", "typeB1")
        leafNode("b2", "typeB2")
      }
      leafNode("c", "typeC")
    }

    val traversalOrder = depthFirstTraversal(tree) { children }.toList()

    traversalOrder.map { it.name } shouldContainExactly listOf(
      "root",
      "a",
      "a1",
      "a2",
      "b",
      "b1",
      "b2",
      "c"
    )
  }

  @Test
  fun `breadthFirstTraversal should return nodes in the correct order`() {
    val tree = rootNode("root", "rootType") {
      compositeNode("a", "typeA") {
        leafNode("a1", "typeA1")
        leafNode("a2", "typeA2")
      }
      compositeNode("b", "typeB") {
        leafNode("b1", "typeB1")
        leafNode("b2", "typeB2")
      }
      leafNode("c", "typeC")
    }

    val traversalOrder = breadthFirstTraversal(tree) { children }.toList()

    traversalOrder.map { it.name } shouldContainExactly listOf(
      "root",
      "a",
      "b",
      "c",
      "a1",
      "a2",
      "b1",
      "b2"
    )
  }
}
