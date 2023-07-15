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

package modulecheck.project.test

import modulecheck.project.ProjectCache
import modulecheck.testing.BaseTest
import modulecheck.testing.TestEnvironmentParams
import java.io.File
import java.nio.charset.Charset

/**
 * Base test class for tests related to projects. Provides
 * useful utility functions for setting up project-related tests.
 */
@Suppress("UnnecessaryAbstractClass")
abstract class ProjectTest<T : ProjectTestEnvironment> : BaseTest<T>() {

  override fun newTestEnvironment(params: TestEnvironmentParams): T {

    val environment = when (params) {
      is ProjectTestEnvironmentParams -> ProjectTestEnvironment(params)
      else -> ProjectTestEnvironment(
        ProjectTestEnvironmentParams(
          projectCache = ProjectCache(),
          testStackFrame = params.testStackFrame,
          testVariantNames = params.testVariantNames
        )
      )
    }
    @Suppress("UNCHECKED_CAST")
    return environment as T
  }

  /**
   * Writes the specified content to the receiver [File] with the default charset.
   *
   * @param content the content to be written to the file
   */
  fun File.writeText(content: String) {
    writeText(content.trimIndent(), Charset.defaultCharset())
  }
}
