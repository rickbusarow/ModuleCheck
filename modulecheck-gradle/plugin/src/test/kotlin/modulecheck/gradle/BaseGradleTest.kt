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

package modulecheck.gradle

import com.rickbusarow.kase.HasKaseMatrix
import com.rickbusarow.kase.HasParams
import com.rickbusarow.kase.asClueCatching
import com.rickbusarow.kase.files.TestLocation
import kotlinx.coroutines.runBlocking
import modulecheck.testing.McTestVersions
import modulecheck.testing.McVersionMatrix
import modulecheck.testing.VersionsFactoryTest
import modulecheck.testing.assertions.TrimmedAsserts
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import org.junit.jupiter.api.parallel.ResourceLock

@Suppress("UnnecessaryAbstractClass")
@ResourceLock("Gradle")
@Execution(SAME_THREAD)
abstract class BaseGradleTest(
  final override val kaseMatrix: McVersionMatrix = McVersionMatrix()
) : VersionsFactoryTest<McGradleTestEnvironment, McGradleTestEnvironmentFactory>,
  HasParams<McTestVersions>,
  TrimmedAsserts,
  HasKaseMatrix {

  override val params: List<McTestVersions> by lazy { versions() }

  override val testEnvironmentFactory = McGradleTestEnvironmentFactory()

  /** shorthand for executing a test in a hermetic TestEnvironment but without any kase parameters */
  fun test(
    testLocation: TestLocation = TestLocation.get(),
    testAction: suspend McGradleTestEnvironment.() -> Unit
  ) {
    val testEnvironment = testEnvironmentFactory.create(
      params = params.last(),
      names = emptyList(),
      location = testLocation
    )

    runBlocking {
      testEnvironment.asClueCatching {
        testEnvironment.testAction()
        println(testEnvironment)
      }
    }
  }
}
