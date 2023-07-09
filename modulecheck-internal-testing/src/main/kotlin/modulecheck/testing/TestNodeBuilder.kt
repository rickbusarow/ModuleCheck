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

import io.kotest.property.Arb
import io.kotest.property.RandomSource
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import java.io.File
import java.lang.StackWalker.StackFrame
import java.net.URI
import java.nio.file.Paths
import java.util.stream.Stream
import kotlin.io.path.div
import kotlin.streams.asStream

/**
 * Helper function to build a dynamic test factory with specified initialization logic.
 *
 * Example usage:
 * ```
 * @TestFactory
 * fun `some test`() = testFactory {
 *   test("Test1") {
 *     println("Executing Test1")
 *   }
 * }
 * ```
 *
 * @param init a lambda with receiver that initializes the [TestNodeBuilder].
 * @return a stream of dynamic nodes constructed by the test factory builder.
 */
@SkipInStackTrace
fun testFactory(init: TestNodeBuilder.() -> Unit): Stream<out DynamicNode> {
  return TestNodeBuilder(
    name = "root",
    rootStackFrame = HasWorkingDir.testStackFrame(),
    parent = null
  )
    .apply { init() }
    .nodeSequence()
    .asStream()
}

/**
 * Builds a dynamic test container with a specific name and a list of dynamic
 * nodes (tests or containers). Provides functions for defining and adding
 * dynamic tests and containers to the nodes list. Each node within the
 * container can provide a list of names starting from the root container.
 *
 * Example usage:
 * ```
 * @TestFactory
 * fun `some test`() = testFactory {
 *   test("Test1") {
 *     println("Executing Test1")
 *   }
 * }
 * ```
 *
 * @property name the name of the test container.
 * @property rootStackFrame Captured before executing any tests,
 *   meaning that it's the frame which called `testFactory { ... }`
 * @property parent the parent node, or `null` if this is the root container
 */
class TestNodeBuilder @PublishedApi internal constructor(
  val name: String,
  val rootStackFrame: StackFrame,
  val parent: TestNodeBuilder?
) {
  /** the list of names from the root node to this node */
  val namesFromRoot: List<String>
    get() = if (parent == null) {
      listOf(name)
    } else {
      parent.namesFromRoot + name
    }

  @PublishedApi
  internal val nodes: MutableList<() -> DynamicNode> = mutableListOf()

  internal fun nodeSequence(): Sequence<DynamicNode> = nodes.asSequence().map { it() }

  @PublishedApi
  internal fun testUri(): URI {

    val here = Paths.get("").toAbsolutePath()
    val srcTestKotlin = here / "src/test/kotlin"
    val packageDir = rootStackFrame.declaringClass.packageName
      .replace('.', File.separatorChar)
    val classFile = srcTestKotlin / packageDir / rootStackFrame.fileName
    val lineNumber = rootStackFrame.lineNumber

    return URI.create(File("$classFile").toURI().toString() + "?line=$lineNumber")
  }

  /**
   * Creates a dynamic test with the provided name and test logic, adds it to the nodes list.
   *
   * @param name the name of the test.
   * @param action a function containing the test logic.
   */
  inline fun test(name: String, crossinline action: () -> Unit) {
    addTest(name, action)
  }

  /**
   * Creates a dynamic test with the provided name and test logic, adds it to the nodes list.
   *
   * @param name the name of the test.
   * @param action a function containing the test logic.
   */
  context(HasTestEnvironment<T>)
  inline fun <T : TestEnvironment> test(name: String, crossinline action: suspend T.() -> Unit) {
    addTest(name) {
      this@HasTestEnvironment.test(
        testStackFrame = rootStackFrame,
        testVariantNames = namesFromRoot + name
      ) { action() }
    }
  }

  @PublishedApi
  internal inline fun addTest(name: String, crossinline action: () -> Unit) {
    nodes.add { DynamicTest.dynamicTest(name, testUri()) { action() } }
  }

  /**
   * Creates a dynamic test with the provided name and test logic, adds it to the nodes list.
   *
   * @param testName the name of the test.
   * @param action a function containing the test logic.
   */
  context(HasTestEnvironment<T>)
  inline fun <T : TestEnvironment, E> Iterable<E>.asTests(
    crossinline testName: (E) -> String = { it.toString() },
    crossinline action: suspend T.(E) -> Unit
  ): TestNodeBuilder = this@TestNodeBuilder.apply {
    for (element in this@asTests) {
      test(testName(element)) { action(element) }
    }
  }

  /**
   * Creates a dynamic test with the provided name and test logic, adds it to the nodes list.
   *
   * @param testName the name of the test.
   * @param action a function containing the test logic.
   */
  context(HasTestEnvironment<T>)
  inline fun <T : TestEnvironment, E> Sequence<E>.asTests(
    crossinline testName: (E) -> String = { it.toString() },
    crossinline action: suspend T.(E) -> Unit
  ): TestNodeBuilder = this@TestNodeBuilder.apply {
    for (element in this@asTests) {
      test(testName(element)) { action(element) }
    }
  }

  /**
   * Adds tests to the invoking [TestNodeBuilder] for each element of the
   * iterable. The names of the tests are determined by the [testName]
   * function, and the tests themselves are defined by the [action] function.
   *
   * @param testName a function to compute the name of each test.
   * @param action a function to define each test.
   * @receiver the [TestNodeBuilder] to which tests will be added.
   * @return the invoking [TestNodeBuilder], after adding the new tests.
   */
  inline fun <E> Iterable<E>.asTests(
    crossinline testName: (E) -> String = { it.toString() },
    crossinline action: (E) -> Unit
  ): TestNodeBuilder = this@TestNodeBuilder.apply {
    for (element in this@asTests) {
      test(testName(element)) { action(element) }
    }
  }

  /**
   * Adds tests to the invoking [TestNodeBuilder] for each element of the
   * iterable. The names of the tests are determined by the [testName]
   * function, and the tests themselves are defined by the [action] function.
   *
   * @param testName a function to compute the name of each test.
   * @param action a function to define each test.
   * @receiver the [TestNodeBuilder] to which tests will be added.
   * @return the invoking [TestNodeBuilder], after adding the new tests.
   */
  inline fun <E> Sequence<E>.asTests(
    crossinline testName: (E) -> String = { it.toString() },
    crossinline action: (E) -> Unit
  ): TestNodeBuilder = this@TestNodeBuilder.apply {
    for (element in this@asTests) {
      test(testName(element)) { action(element) }
    }
  }

  /**
   * Adds containers to the invoking [TestNodeBuilder] for each element of the
   * iterable. The names of the containers are determined by the [testName] function,
   * and the containers themselves are initialized by the [action] function.
   *
   * @param testName a function to compute the name of each container.
   * @param action a function to initialize each container.
   * @receiver the [TestNodeBuilder] to which containers will be added.
   * @return the invoking [TestNodeBuilder], after adding the new containers.
   */
  inline fun <E> Iterable<E>.asContainers(
    testName: (E) -> String = { it.toString() },
    crossinline action: TestNodeBuilder.(E) -> Unit
  ): TestNodeBuilder = this@TestNodeBuilder.apply {
    for (element in this@asContainers) {
      container(testName(element)) { action(element) }
    }
  }

  /**
   * Adds containers to the invoking [TestNodeBuilder] for each element of the
   * iterable. The names of the containers are determined by the [testName] function,
   * and the containers themselves are initialized by the [action] function.
   *
   * @param testName a function to compute the name of each container.
   * @param action a function to initialize each container.
   * @receiver the [TestNodeBuilder] to which containers will be added.
   * @return the invoking [TestNodeBuilder], after adding the new containers.
   */
  inline fun <E> Sequence<E>.asContainers(
    testName: (E) -> String = { it.toString() },
    crossinline action: TestNodeBuilder.(E) -> Unit
  ): TestNodeBuilder = this@TestNodeBuilder.apply {
    for (element in this@asContainers) {
      container(testName(element)) { action(element) }
    }
  }

  /**
   * Creates a dynamic container with the provided name
   * and initialization logic, adds it to the nodes list.
   *
   * @param name the name of the container.
   * @param init a lambda with receiver that initializes the [TestNodeBuilder].
   */
  inline fun container(name: String, crossinline init: TestNodeBuilder.() -> Unit) {

    nodes.add {
      TestNodeBuilder(
        name = name,
        rootStackFrame = rootStackFrame,
        parent = this
      )
        .apply(init)
        .build()
    }
  }

  /**
   * Builds the test container from the current state of this builder.
   *
   * @return a dynamic container with the defined name and nodes.
   */
  @PublishedApi
  internal fun build(): DynamicContainer {
    return DynamicContainer.dynamicContainer(name, nodeSequence().asStream())
  }
}

/**
 * Transforms an iterable into a stream of dynamic test containers. The
 * names of the containers are determined by the [testName] function, and
 * the containers themselves are initialized by the [action] function.
 *
 * @param testName a function to compute the name of each test container.
 * @param action a function to initialize each test container.
 * @return a stream of dynamic nodes representing the containers.
 */
@SkipInStackTrace
inline fun <E> Iterable<E>.asContainers(
  crossinline testName: (E) -> String = { it.toString() },
  crossinline action: TestNodeBuilder.(E) -> Unit
): Stream<out DynamicNode> = testFactory { asContainers(testName, action) }

/**
 * Transforms an iterable into a stream of dynamic test containers. The
 * names of the containers are determined by the [testName] function, and
 * the containers themselves are initialized by the [action] function.
 *
 * @param testName a function to compute the name of each test container.
 * @param action a function to initialize each test container.
 * @return a stream of dynamic nodes representing the containers.
 */
@SkipInStackTrace
inline fun <E> Sequence<E>.asContainers(
  crossinline testName: (E) -> String = { it.toString() },
  crossinline action: TestNodeBuilder.(E) -> Unit
): Stream<out DynamicNode> = testFactory { asContainers(testName, action) }

/**
 * Transforms an iterable into a stream of dynamic tests. The names of the tests are determined
 * by the [testName] function, and the tests themselves are defined by the [action] function.
 *
 * @param testName a function to compute the name of each test.
 * @param action a function to define each test.
 * @return a stream of dynamic nodes representing the tests.
 */
@SkipInStackTrace
inline fun <E> Iterable<E>.asTests(
  crossinline testName: (E) -> String = { it.toString() },
  crossinline action: (E) -> Unit
): Stream<out DynamicNode> = testFactory { asTests(testName, action) }

/**
 * Transforms an iterable into a stream of dynamic tests. The names of the tests are determined
 * by the [testName] function, and the tests themselves are defined by the [action] function.
 *
 * @param testName a function to compute the name of each test.
 * @param action a function to define each test.
 * @return a stream of dynamic nodes representing the tests.
 */
@SkipInStackTrace
inline fun <E> Sequence<E>.asTests(
  crossinline testName: (E) -> String = { it.toString() },
  crossinline action: (E) -> Unit
): Stream<out DynamicNode> = testFactory { asTests(testName, action) }

/** shorthand for `take(count = count, rs = rs).asTests(testName, action)` */
@SkipInStackTrace
inline fun <E> Arb<E>.asTests(
  count: Int = 100,
  rs: RandomSource = RandomSource.default(),
  crossinline testName: (E) -> String = { it.toString() },
  crossinline action: (E) -> Unit
): Stream<out DynamicNode> = samples(rs)
  .map { it.value }
  .distinct()
  .take(count)
  .asTests(testName, action)

/**
 * Transforms an iterable into a stream of dynamic tests. The names of the tests are determined
 * by the [testName] function, and the tests themselves are defined by the [action] function.
 *
 * @param testName a function to compute the name of each test.
 * @param action a function to define each test.
 * @return a stream of dynamic nodes representing the tests.
 */
context(HasTestEnvironment<T>)
@SkipInStackTrace
inline fun <T : TestEnvironment, E> Iterable<E>.asTests(
  crossinline testName: (E) -> String = { it.toString() },
  crossinline action: suspend T.(E) -> Unit
): Stream<out DynamicNode> = testFactory { asTests(testName, action) }

/**
 * Transforms an iterable into a stream of dynamic tests. The names of the tests are determined
 * by the [testName] function, and the tests themselves are defined by the [action] function.
 *
 * @param testName a function to compute the name of each test.
 * @param action a function to define each test.
 * @return a stream of dynamic nodes representing the tests.
 */
context(HasTestEnvironment<T>)
@SkipInStackTrace
inline fun <T : TestEnvironment, E> Sequence<E>.asTests(
  crossinline testName: (E) -> String = { it.toString() },
  crossinline action: suspend T.(E) -> Unit
): Stream<out DynamicNode> = testFactory { asTests(testName, action) }
