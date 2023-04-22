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

import io.kotest.matchers.shouldBe
import modulecheck.utils.traversal.AbstractTreePrinter.Color.Companion.noColors
import org.junit.jupiter.api.Test

class AbstractTreePrinterTest {
  @Test
  fun `tree string output is nested`() {
    val tree = rootNode("root", "root") {
      compositeNode("a", "composite") {
        leafNode("a1", "leaf")
        leafNode("a2", "leaf")
      }
      compositeNode("b", "composite") {
        leafNode("b1", "leaf")
        leafNode("b2", "leaf")
      }
      leafNode("c", "typeC")
    }

    TestNodeTreePrinter().treeString(tree).noColors() shouldBe """
    ┏━ CompositeNode [type: root] [parent: null] [parent type: null] ━┓
    ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
    ┃text for root                                                    ┃
    ┃the element type is root                                         ┃
    ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    ╎  ┏━ CompositeNode (2) [type: composite] [parent: CompositeNode] [parent type: root] ━┓
    ╎  ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
    ╎  ┃text for a                                                                         ┃
    ╎  ┃the element type is composite                                                      ┃
    ╎  ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    ╎  ╎  ┏━ LeafNode [type: leaf] [parent: CompositeNode (2)] [parent type: composite] ━┓
    ╎  ╎  ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
    ╎  ╎  ┃text for a1                                                                   ┃
    ╎  ╎  ┃the element type is leaf                                                      ┃
    ╎  ╎  ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    ╎  ╎  ┏━ LeafNode (2) [type: leaf] [parent: CompositeNode (2)] [parent type: composite] ━┓
    ╎  ╎  ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
    ╎  ╎  ┃text for a2                                                                       ┃
    ╎  ╎  ┃the element type is leaf                                                          ┃
    ╎  ╎  ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    ╎  ┏━ CompositeNode (3) [type: composite] [parent: CompositeNode] [parent type: root] ━┓
    ╎  ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
    ╎  ┃text for b                                                                         ┃
    ╎  ┃the element type is composite                                                      ┃
    ╎  ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    ╎  ╎  ┏━ LeafNode (3) [type: leaf] [parent: CompositeNode (3)] [parent type: composite] ━┓
    ╎  ╎  ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
    ╎  ╎  ┃text for b1                                                                       ┃
    ╎  ╎  ┃the element type is leaf                                                          ┃
    ╎  ╎  ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    ╎  ╎  ┏━ LeafNode (4) [type: leaf] [parent: CompositeNode (3)] [parent type: composite] ━┓
    ╎  ╎  ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
    ╎  ╎  ┃text for b2                                                                       ┃
    ╎  ╎  ┃the element type is leaf                                                          ┃
    ╎  ╎  ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    ╎  ┏━ LeafNode (5) [type: typeC] [parent: CompositeNode] [parent type: root] ━┓
    ╎  ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
    ╎  ┃text for c                                                                ┃
    ╎  ┃the element type is typeC                                                 ┃
    ╎  ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    """.trimIndent()
  }

  @Test
  fun `a custom whitespace character does not pad the entire text box`() {
    val tree = rootNode("root", "root") {
      compositeNode("a", "composite") {
        leafNode("a1", "leaf")
        leafNode("a2", "leaf")
      }
      compositeNode("b", "composite") {
        leafNode("b1", "leaf")
        leafNode("b2", "leaf")
      }
      leafNode("c", "typeC")
    }

    TestNodeTreePrinter('·').treeString(tree).noColors() shouldBe """
    ┏━ CompositeNode [type: root] [parent: null] [parent type: null] ━┓
    ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
    ┃text·for·root                                                    ┃
    ┃the·element·type·is·root                                         ┃
    ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    ╎  ┏━ CompositeNode (2) [type: composite] [parent: CompositeNode] [parent type: root] ━┓
    ╎  ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
    ╎  ┃text·for·a                                                                         ┃
    ╎  ┃the·element·type·is·composite                                                      ┃
    ╎  ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    ╎  ╎  ┏━ LeafNode [type: leaf] [parent: CompositeNode (2)] [parent type: composite] ━┓
    ╎  ╎  ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
    ╎  ╎  ┃text·for·a1                                                                   ┃
    ╎  ╎  ┃the·element·type·is·leaf                                                      ┃
    ╎  ╎  ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    ╎  ╎  ┏━ LeafNode (2) [type: leaf] [parent: CompositeNode (2)] [parent type: composite] ━┓
    ╎  ╎  ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
    ╎  ╎  ┃text·for·a2                                                                       ┃
    ╎  ╎  ┃the·element·type·is·leaf                                                          ┃
    ╎  ╎  ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    ╎  ┏━ CompositeNode (3) [type: composite] [parent: CompositeNode] [parent type: root] ━┓
    ╎  ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
    ╎  ┃text·for·b                                                                         ┃
    ╎  ┃the·element·type·is·composite                                                      ┃
    ╎  ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    ╎  ╎  ┏━ LeafNode (3) [type: leaf] [parent: CompositeNode (3)] [parent type: composite] ━┓
    ╎  ╎  ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
    ╎  ╎  ┃text·for·b1                                                                       ┃
    ╎  ╎  ┃the·element·type·is·leaf                                                          ┃
    ╎  ╎  ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    ╎  ╎  ┏━ LeafNode (4) [type: leaf] [parent: CompositeNode (3)] [parent type: composite] ━┓
    ╎  ╎  ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
    ╎  ╎  ┃text·for·b2                                                                       ┃
    ╎  ╎  ┃the·element·type·is·leaf                                                          ┃
    ╎  ╎  ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    ╎  ┏━ LeafNode (5) [type: typeC] [parent: CompositeNode] [parent type: root] ━┓
    ╎  ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
    ╎  ┃text·for·c                                                                ┃
    ╎  ┃the·element·type·is·typeC                                                 ┃
    ╎  ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    """.trimIndent()
  }
}
