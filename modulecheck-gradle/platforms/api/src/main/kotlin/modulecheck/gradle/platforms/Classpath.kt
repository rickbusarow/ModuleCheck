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

package modulecheck.gradle.platforms

import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.gradle.model.GradleProject
import modulecheck.project.McProject
import modulecheck.utils.requireExists
import java.io.File

@JvmInline
value class Classpath(val files: List<File>) {
  companion object {

    fun reportFile(project: GradleProject, sourceSetName: SourceSetName): File {
      return project.buildDir.reportFile(sourceSetName)
    }

    fun reportFile(project: McProject, sourceSetName: SourceSetName): File {
      return project.projectDir.resolve("build").reportFile(sourceSetName)
    }

    private fun File.reportFile(sourceSetName: SourceSetName): File {
      return resolve("outputs/modulecheck/classpath/${sourceSetName.value}.txt")
    }

    fun from(project: GradleProject, sourceSetName: SourceSetName): Classpath {
      return reportFile(project, sourceSetName).parseToClasspath()
    }

    fun from(project: McProject, sourceSetName: SourceSetName): Classpath {
      return reportFile(project, sourceSetName).parseToClasspath()
    }

    private fun File.parseToClasspath(): Classpath {
      requireExists { "The expected classpath report file is missing at $absolutePath" }

      val files = readText()
        .lineSequence()
        .filter { it.isNotBlank() }
        .map { File(it) }
        .onEach { it.requireExists() }

      return Classpath(files.toList())
    }
  }
}
