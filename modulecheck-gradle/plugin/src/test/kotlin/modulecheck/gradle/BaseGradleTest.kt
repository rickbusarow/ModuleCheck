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

package modulecheck.gradle

import modulecheck.project.ProjectCache
import modulecheck.testing.BaseTest
import modulecheck.testing.DynamicTests
import modulecheck.testing.SkipInStackTrace
import modulecheck.testing.TestEnvironmentParams
import modulecheck.testing.TestVersions
import modulecheck.testing.VersionsFactoryTest
import modulecheck.utils.remove
import org.junit.jupiter.api.DynamicTest
import java.lang.StackWalker.StackFrame

@Suppress("UnnecessaryAbstractClass")
abstract class BaseGradleTest :
  BaseTest<GradleTestEnvironment>(),
  DynamicTests,
  VersionsFactoryTest<GradleTestEnvironment> {

  override fun newTestEnvironment(params: TestEnvironmentParams): GradleTestEnvironment {

    return when (params) {
      is GradleTestEnvironmentParams -> GradleTestEnvironment(params)
      else -> GradleTestEnvironment(
        GradleTestEnvironmentParams(
          testVersions = defaultTestVersions(),
          projectCache = ProjectCache(),
          testStackFrame = params.testStackFrame,
          testVariantNames = params.testVariantNames
        )
      )
    }
  }

  @SkipInStackTrace
  final override fun dynamicTest(
    subject: TestVersions,
    stackFrame: StackFrame,
    testName: String,
    action: suspend GradleTestEnvironment.() -> Unit
  ): DynamicTest = DynamicTest.dynamicTest(testName) {

    test(
      params = GradleTestEnvironmentParams(
        testVersions = subject,
        projectCache = ProjectCache(),
        testStackFrame = stackFrame,
        testVariantNames = testName
          .substringAfter('[')
          .substringBefore(']')
          .split(',')
          .map { it.trim().replace(" ", "_").remove(":") }
      ),
      action = action
    )
  }
}
