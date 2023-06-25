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

package modulecheck.testing

import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import java.util.LinkedList
import java.util.stream.Stream
import kotlin.streams.toList

class TestNodeBuilderTest : HasTestEnvironment<TestEnvironment> {

  @Test
  fun `testFactory creates single test node`() {
    val dynamicNodesStream = testFactory {
      test("Test1") {}
    }

    val dynamicNodes = dynamicNodesStream.toList()
    dynamicNodes.size shouldBe 1

    val testNode = dynamicNodes.single()
    testNode.shouldBeInstanceOf<DynamicTest>()
    testNode.displayName shouldBe "Test1"
  }

  @Test
  fun `testFactory creates multiple test nodes`() {
    val dynamicNodesStream = testFactory {
      test("Test1") {}
      test("Test2") {}
    }

    val dynamicNodes = dynamicNodesStream.toList()
    dynamicNodes shouldHaveSize 2

    dynamicNodes.shouldForAll { it.shouldBeInstanceOf<DynamicTest>() }

    val testNames = dynamicNodes.names()
    testNames shouldContainExactly listOf("Test1", "Test2")
  }

  @Test
  fun `testFactory creates a dynamic container`() {
    val dynamicNodesStream = testFactory {
      container("Container1") {
        test("Test3") {}
      }
    }

    val dynamicNodes = dynamicNodesStream.toList()
    dynamicNodes shouldHaveSize 1

    val container = dynamicNodes.first()
    container.shouldBeInstanceOf<DynamicContainer>()
    container.displayName shouldBe "Container1"

    val containerChildren = container.children.toList()
    containerChildren shouldHaveSize 1

    val testNode = containerChildren.first()
    testNode.shouldBeInstanceOf<DynamicTest>()
    testNode.displayName shouldBe "Test3"
  }

  @Test
  fun `TestNodeBuilder asTests creates dynamic tests`() {
    val elements = listOf("Element1", "Element2")

    val dynamicNodesStream = testFactory {
      elements.asTests({ "Test $it" }) {}
    }

    val dynamicNodes = dynamicNodesStream.toList()
    dynamicNodes shouldHaveSize 2

    val testNames = dynamicNodes.map { it.displayName }
    testNames shouldContainExactly listOf("Test Element1", "Test Element2")
  }

  @Test
  fun `TestNodeBuilder asContainers creates dynamic containers`() {
    val elements = listOf("Element1", "Element2")

    val dynamicNodesStream = testFactory {
      elements.asContainers({ "Container $it" }) {}
    }

    val dynamicNodes = dynamicNodesStream.toList()
    dynamicNodes shouldHaveSize 2

    val containerNames = dynamicNodes.map { it.displayName }
    containerNames shouldContainExactly listOf("Container Element1", "Container Element2")
  }

  @Test
  fun `Iterable asTests extension creates dynamic tests`() {
    val elements = listOf("Element1", "Element2")

    val dynamicNodesStream = elements.asTests({ "Test $it" }) {}

    val dynamicNodes = dynamicNodesStream.toList()
    dynamicNodes shouldHaveSize 2

    val testNames = dynamicNodes.map { it.displayName }
    testNames shouldContainExactly listOf("Test Element1", "Test Element2")
  }

  @Test
  fun `iterable asContainers adds items as containers`() {
    val items = listOf("Item1", "Item2")
    val dynamicNodes = items.asContainers(testName = { it }, action = { }).toList()

    dynamicNodes.names() shouldBe listOf("Item1", "Item2")
  }

  @Test
  fun `iterable asTests adds items as tests`() {
    val items = listOf("Item1", "Item2")
    val dynamicNodes = items.asTests(testName = { it }, action = { }).toList()

    dynamicNodes.names() shouldBe listOf("Item1", "Item2")
  }

  @Test
  fun `root node builder creates containers using testFactory DSL`() {
    val dynamicNodes = testFactory {
      container("Container1") { }
      container("Container2") { }
    }

    dynamicNodes.names() shouldBe listOf("Container1", "Container2")
  }

  @Test
  fun `container builder adds nested containers to a container using testFactory DSL`() {
    val dynamicContainer = testFactory {
      container("Container1") {
        container("Container2") { }
        container("Container3") { }
      }
    }
      .findFirst().get() as DynamicContainer

    dynamicContainer.children
      .names() shouldBe listOf("Container2", "Container3")
  }

  @Test
  fun `container builder adds tests and containers to a container using testFactory DSL`() {
    val dynamicContainer = testFactory {
      container("Container1") {
        test("Test1") { }
        container("Container2") { }
        test("Test2") { }
        container("Container3") { }
      }
    }
      .findFirst().get() as DynamicContainer

    dynamicContainer.children
      .names() shouldBe listOf("Test1", "Container2", "Test2", "Container3")
  }

  @Test
  fun `root node builder adds tests and containers using testFactory DSL`() {
    val dynamicNodes = testFactory {
      test("Test1") { }
      container("Container1") { }
      test("Test2") { }
      container("Container2") { }
    }

    dynamicNodes.names() shouldBe listOf("Test1", "Container1", "Test2", "Container2")
  }

  @Test
  fun `leaf node has stackFrame from root node`() {

    var invoked = false

    val stackFrameFromHere = HasWorkingDir.testStackFrame()
    val dynamicNodes = testFactory {
      container("Container") {
        test("Test") {

          // The expected stackFrame is from one line before the call to `testFactory { }`,
          // so the line number in the string will be different.
          // Parse out the line number from the end of the line, add one,
          // and update the line with the new value.
          // That should match the stackFrame created in the DSL.
          val expected = stackFrameFromHere.toString()
            .replace("""(\d+)\)$""".toRegex()) { mr ->
              val value = mr.groupValues[1].toInt() + 1
              "$value)"
            }

          rootStackFrame.toString() shouldBe expected
          invoked = true
        }
      }
    }
      .allNodes()

    dynamicNodes.allTests().single().executable.execute()

    invoked shouldBe true

    dynamicNodes.names() shouldBe listOf("Container", "Test")
  }

  private fun Stream<out DynamicNode>.allNodes(): List<DynamicNode> {
    return toList().allNodes()
  }

  private fun List<DynamicNode>.allNodes(): List<DynamicNode> {
    val result = mutableListOf<DynamicNode>()
    val nodesToVisit = LinkedList(this)
    while (nodesToVisit.isNotEmpty()) {
      val node = nodesToVisit.removeFirst()
      result.add(node)
      if (node is DynamicContainer) {
        nodesToVisit.addAll(0, node.children.toList())
      }
    }
    return result
  }

  private fun List<DynamicNode>.allTests(): List<DynamicTest> = filterIsInstance<DynamicTest>()
  private fun Stream<out DynamicNode>.names() = toList().map { it.displayName }
  private fun List<DynamicNode>.names() = map { it.displayName }
}
