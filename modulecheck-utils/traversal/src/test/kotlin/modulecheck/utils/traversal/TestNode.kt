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

sealed interface TestNode {
  val name: String
  val elementType: String
  val children: List<TestNode>
  var parent: TestNode?

  val text: String get() = "text for $name\nthe element type is $elementType"
}

data class CompositeNode(
  override val name: String,
  override val elementType: String,
  override val children: List<TestNode>
) : TestNode {
  override var parent: TestNode? = null
}

data class LeafNode(
  override val name: String,
  override val elementType: String
) : TestNode {
  override var parent: TestNode? = null
  override val children: List<TestNode>
    get() = listOf()
}

fun rootNode(name: String, elementType: String, init: CompositeNodeBuilder.() -> Unit): TestNode {
  val builder = CompositeNodeBuilder(name, elementType, null)
  builder.init()
  return builder.build()
}

class CompositeNodeBuilder(
  private val name: String,
  private val elementType: String,
  private val parent: TestNode?
) {
  private val children = mutableListOf<TestNode>()

  fun compositeNode(name: String, elementType: String, init: CompositeNodeBuilder.() -> Unit) {
    val childBuilder = CompositeNodeBuilder(name, elementType, build())
    childBuilder.init()
    children.add(childBuilder.build())
  }

  fun leafNode(name: String, elementType: String) {
    children.add(LeafNode(name, elementType).also { it.parent = build() })
  }

  fun build(): TestNode = CompositeNode(name, elementType, children).apply {
    this@apply.parent = this@CompositeNodeBuilder.parent
    children.forEach { it.parent = this }
  }
}

class TestNodeTreePrinter(
  whitespaceChar: Char = ' '
) : AbstractTreePrinter<TestNode>(whitespaceChar) {
  override fun TestNode.simpleClassName(): String = this::class.java.simpleName
  override fun TestNode.parent(): TestNode? = parent
  override fun TestNode.typeName(): String = elementType

  override fun TestNode.text(): String = text
  override fun TestNode.children() = children.asSequence()
}
