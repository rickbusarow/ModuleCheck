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

package modulecheck.testing

import com.rickbusarow.kase.HasKaseMatrix
import com.rickbusarow.kase.KaseTestFactory
import com.rickbusarow.kase.TestEnvironmentFactory
import com.rickbusarow.kase.TestNodeBuilder
import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldNotBeEmpty
import modulecheck.utils.letIf
import modulecheck.utils.requireNotNull
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import java.util.stream.Stream

/**
 * Convenience interface for a test which uses [KaseTestFactory]
 * in order to create [DynamicTest]s for a JUnit5 test factory.
 */
interface VersionsFactoryTest<ENV, FACT> :
  KaseTestFactory<McTestVersions, ENV, FACT>,
  HasKaseMatrix
  where ENV : TestEnvironment,
        ENV : HasTestVersions,
        FACT : TestEnvironmentFactory<McTestVersions, ENV> {

  /** */
  override val kaseMatrix: McVersionMatrix

  /** If false, then tests will only use the latest version of each dependency */
  val exhaustive: Boolean
    get() = Versions.exhaustive

  /** @return the latest version of valid dependencies which is not excluded by the current rules */
  fun defaultTestVersions(): McTestVersions = kaseMatrix.allVersions.last()

  /**
   * @return a stream of [DynamicTest] from all valid versions combinations,
   *   optionally filtered by [filter]. [action] is performed against each element.
   */
  @SkipInStackTrace
  fun factory(
    exhaustive: Boolean = this.exhaustive,
    filter: ((McTestVersions) -> Boolean)? = null,
    testEnvironmentFactory: TestEnvironmentFactory<McTestVersions, ENV> =
      this@VersionsFactoryTest.testEnvironmentFactory,
    testAction: suspend ENV.(McTestVersions) -> Unit
  ): Stream<out DynamicNode> = testVersionsPrivate(exhaustive, filter)
    .asTests(
      testEnvironmentFactory = testEnvironmentFactory,
      testAction = testAction
    )

  /**
   * @return a stream of [DynamicNode] from all valid versions combinations,
   *   optionally filtered by [filter]. [builder] is performed against each element.
   */
  @SkipInStackTrace
  fun factoryContainers(
    exhaustive: Boolean = this.exhaustive,
    filter: ((McTestVersions) -> Boolean)? = null,
    builder: TestNodeBuilder.(McTestVersions) -> Stream<out DynamicNode>
  ): Stream<out DynamicNode> = testVersionsPrivate(exhaustive, filter)
    .asContainers { testVersions -> builder(testVersions) }

  /** either all permutations or just the last */
  fun versions(exhaustive: Boolean = this.exhaustive): List<McTestVersions> {
    return kaseMatrix.allVersions.letIf(!exhaustive) { it.takeLast(1) }
  }
}

@PublishedApi
internal fun VersionsFactoryTest<*, *>.testVersionsPrivate(
  exhaustive: Boolean,
  filter: ((McTestVersions) -> Boolean)?
): List<McTestVersions> = kaseMatrix.versions(exhaustive = exhaustive)
  .letIf(filter != null) { versions ->

    val (included, excluded) = kaseMatrix.allVersions
      .partition(filter.requireNotNull())

    "The filter excludes all possible versions".asClue {
      included.shouldNotBeEmpty()
    }

    "The filter does not exclude any versions".asClue {
      excluded.shouldNotBeEmpty()
    }

    versions.filter(filter.requireNotNull())
  }
