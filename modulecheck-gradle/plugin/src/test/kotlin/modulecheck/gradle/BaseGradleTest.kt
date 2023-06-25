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
import modulecheck.testing.TestEnvironmentParams
import modulecheck.testing.TestVersions
import modulecheck.testing.VersionsFactoryTest
import modulecheck.utils.remove
import java.lang.StackWalker.StackFrame

@Suppress("UnnecessaryAbstractClass")
abstract class BaseGradleTest :
  BaseTest<GradleTestEnvironment>(),
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

  final override fun TestVersions.newParams(stackFrame: StackFrame): GradleTestEnvironmentParams {
    return GradleTestEnvironmentParams(
      testVersions = this,
      projectCache = ProjectCache(),
      testStackFrame = stackFrame,
      testVariantNames = this.toString()
        .substringAfter('[')
        .substringBefore(']')
        .split(',')
        .map { it.trim().replace(" ", "_").remove(":") }
    )
  }
}
