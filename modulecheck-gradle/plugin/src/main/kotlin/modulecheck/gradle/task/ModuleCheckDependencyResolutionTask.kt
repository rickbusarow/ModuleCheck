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

package modulecheck.gradle.task

import com.android.builder.aar.AarExtractor
import modulecheck.gradle.internal.configuring
import modulecheck.gradle.platforms.Classpath
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.gradle.model.GradleProject
import modulecheck.utils.capitalize
import modulecheck.utils.createSafely
import modulecheck.utils.mkdirsInline
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import java.io.File

abstract class ModuleCheckDependencyResolutionTask : ModuleCheckSourceSetTask() {

  init {
    description = "Resolves all external dependencies"
  }

  @get:Internal
  abstract val sourceSetNameProp: Property<SourceSetName>

  @get:Internal
  override val sourceSetName: SourceSetName
    get() = sourceSetNameProp.get()

  @get:OutputFile
  abstract val classpathFile: RegularFileProperty

  @TaskAction
  fun execute() {

    val unzippedDir = classpathFile.asFile.get()
      .resolveSibling("unzipped")

    fun File.unzippedAarDir(): File {
      return unzippedDir.resolve(nameWithoutExtension)
    }

    inputs.files
      .flatMap { javaFile ->
        if (javaFile.isDirectory) {
          javaFile.walkBottomUp().filter { it.isFile }.toList()
        } else {
          listOf(javaFile)
        }
      }
      .filter { it.name.endsWith(".aar") }
      .forEach { aar ->

        val dir = aar.unzippedAarDir()

        if (dir.list().isNullOrEmpty()) {
          AarExtractor().extract(aar, dir.mkdirsInline())
        }
      }

    inputs.files
      .flatMap { file ->
        when {
          file.isDirectory -> {
            file.walkBottomUp().filter { it.isFile }.toList()
          }

          else -> {
            listOf(file)
          }
        }
      }
      .plus(
        unzippedDir
          .walkBottomUp()
          .filter { it.isFile && it.extension == "jar" }
          .toList()
      )
      .distinct()
      .sorted()
      .joinToString("\n")
      .also { txt ->
        classpathFile.asFile.get()
          .createSafely(txt)
      }
  }

  companion object {
    fun register(
      project: GradleProject,
      sourceSetName: SourceSetName
    ): TaskProvider<ModuleCheckDependencyResolutionTask> {
      val sourceSetCaps = sourceSetName.value.capitalize()
      return project.tasks.register(
        "resolve${sourceSetCaps}NonProjectDependencies",
        ModuleCheckDependencyResolutionTask::class.java
      ).configuring { task ->

        task.classpathFile.set(Classpath.reportFile(project, sourceSetName))
        task.sourceSetNameProp.set(sourceSetName)
      }
    }
  }
}
