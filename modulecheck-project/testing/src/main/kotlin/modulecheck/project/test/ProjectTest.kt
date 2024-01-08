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

package modulecheck.project.test

import com.rickbusarow.kase.HasTestEnvironmentFactory
import com.rickbusarow.kase.asClueCatching
import com.rickbusarow.kase.files.TestLocation
import kotlinx.coroutines.runBlocking
import modulecheck.project.ProjectCache
import java.io.File
import java.nio.charset.Charset

/**
 * Base test class for tests related to projects. Provides
 * useful utility functions for setting up project-related tests.
 */
@Suppress("UnnecessaryAbstractClass")
abstract class ProjectTest : HasTestEnvironmentFactory<ProjectTestEnvironmentFactory> {

  override val testEnvironmentFactory = ProjectTestEnvironmentFactory()

  /** shorthand for executing a test in a hermetic TestEnvironment but without any kase parameters */
  fun test(
    testLocation: TestLocation = TestLocation.get(),
    testAction: suspend ProjectTestEnvironment.() -> Unit
  ) {
    val testEnvironment = testEnvironmentFactory.createEnvironment(
      params = ProjectTestEnvironmentParams(ProjectCache()),
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

  /**
   * Writes the specified content to the receiver [File] with the default charset.
   *
   * @param content the content to be written to the file
   */
  fun File.writeText(content: String) {
    writeText(content.trimIndent(), Charset.defaultCharset())
  }
}
