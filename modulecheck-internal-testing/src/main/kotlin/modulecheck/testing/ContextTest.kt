/*
 * Copyright (C) 2021 Rick Busarow
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

import hermit.test.junit.HermitJUnit5
import hermit.test.resets
import modulecheck.api.*
import modulecheck.api.anvil.AnvilGradlePlugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Suppress("LongParameterList")
abstract class ContextTest : HermitJUnit5(), DynamicTests {

  protected val projectDir by tempDir()

  fun File.relativePath() = path.removePrefix(projectDir.path)

  private var testInfo: TestInfo? = null

  private val projectCache by resets { ConcurrentHashMap<String, Project2>() }

  fun project(
    gradlePath: String = ":",
    projectDir: File = this@ContextTest.projectDir,
    buildFile: File = File(projectDir, "build.gradle.kts"),
    configurations: Map<ConfigurationName, Config> = emptyMap(),
    projectDependencies: Lazy<ProjectDependencies> = lazy { ProjectDependencies(emptyMap()) },
    hasKapt: Boolean = false,
    sourceSets: Map<SourceSetName, SourceSet> = emptyMap(),
    projectCache: ConcurrentHashMap<String, Project2> = this@ContextTest.projectCache,
    anvilGradlePlugin: AnvilGradlePlugin? = null
  ): Project2 {
    return projectCache.getOrPut(gradlePath) {
      Project2Impl(
        path = gradlePath,
        projectDir = projectDir,
        buildFile = buildFile,
        configurations = configurations,
        projectDependencies = projectDependencies,
        hasKapt = hasKapt,
        sourceSets = sourceSets,
        projectCache = projectCache,
        anvilGradlePlugin = anvilGradlePlugin
      )
    }
  }

  @BeforeEach
  fun beforeEach(testInfo: TestInfo) {
    this.testInfo = testInfo
  }

  @AfterEach
  fun afterEach() {
    testInfo = null
  }
}
