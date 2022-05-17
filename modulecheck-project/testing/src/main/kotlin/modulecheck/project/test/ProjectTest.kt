/*
 * Copyright (C) 2021-2022 Rick Busarow
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

import modulecheck.parsing.gradle.model.ProjectPath
import modulecheck.project.McProject
import modulecheck.project.ProjectCache
import modulecheck.project.ProjectProvider
import modulecheck.testing.BaseTest
import java.io.File
import java.nio.charset.Charset

abstract class ProjectTest : BaseTest(), ProjectCollector {

  override val projectCache: ProjectCache by resets { ProjectCache() }

  override val root: File
    get() = testProjectDir

  val projectProvider: ProjectProvider by resets {
    object : ProjectProvider {

      override val projectCache: ProjectCache
        get() = this@ProjectTest.projectCache

      override fun get(path: ProjectPath): McProject {
        return projectCache.getValue(path)
      }

      override fun getAll(): List<McProject> = allProjects()

      override fun clearCaches() {
        allProjects().forEach { it.clearContext() }
      }
    }
  }

  fun allProjects(): List<McProject> = projectCache.values.toList()

  fun File.writeText(content: String) {
    writeText(content.trimIndent(), Charset.defaultCharset())
  }
}
