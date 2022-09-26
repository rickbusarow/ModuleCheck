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

package modulecheck.gradle.task

import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.gradle.model.GradleProject
import modulecheck.project.McProject
import modulecheck.utils.createSafely
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class ModuleCheckDependencyResolutionTask : AbstractModuleCheckTask() {
  init {
    description = "Resolves all external dependencies"
  }

  @get:OutputFile
  abstract val classpathFile: RegularFileProperty

  @TaskAction
  fun execute() {

    inputs.files
      .flatMap { javaFile ->
        if (javaFile.isDirectory) {
          javaFile.walkBottomUp().filter { it.isFile }.toList()
        } else {
          listOf(javaFile)
        }
      }
      .distinct()
      .sorted()
      .joinToString("\n")
      .also { txt ->
        classpathFile.asFile.get()
          .createSafely(txt)
      }
  }

  companion object {
    fun classpathFile(project: GradleProject, sourceSetName: SourceSetName): File {
      return project.buildDir.classpathFile(sourceSetName)
    }

    fun classpathFile(project: McProject, sourceSetName: SourceSetName): File {
      return project.projectDir.resolve("build").classpathFile(sourceSetName)
    }

    private fun File.classpathFile(sourceSetName: SourceSetName): File {
      return resolve("outputs/modulecheck/classpath/${sourceSetName.value}.txt")
    }
  }
}
