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
import modulecheck.gradle.internal.registerOnce
import modulecheck.gradle.platforms.Classpath
import modulecheck.gradle.platforms.internal.GradleProject
import modulecheck.model.sourceset.SourceSetName
import modulecheck.utils.capitalize
import modulecheck.utils.createSafely
import modulecheck.utils.mkdirsInline
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import java.io.File
import org.gradle.api.tasks.Classpath as GradleClasspath

/**
 * Resolves all external dependencies for a given classpath. That
 * resolution must be done on a thread managed by Gradle, such as in a task.
 */
abstract class ModuleCheckDependencyResolutionTask : ModuleCheckSourceSetTask() {

  init {
    description = "Resolves all external dependencies"
  }

  /** */
  @get:InputFiles
  @get:GradleClasspath
  abstract val classpathToResolve: ConfigurableFileCollection

  /** */
  @get:Internal
  abstract val sourceSetNameProp: Property<SourceSetName>

  @get:Internal
  override val sourceSetName: SourceSetName
    get() = sourceSetNameProp.get()

  /** */
  @get:OutputFile
  abstract val classpathReportFile: RegularFileProperty

  /** Executes the task. */
  @TaskAction
  fun execute() {

    val unzippedDir = classpathReportFile.asFile.get()
      .resolveSibling("unzipped")

    fun File.unzippedAarDir(): File {
      return unzippedDir.resolve(nameWithoutExtension)
    }

    classpathToResolve.files
      .asSequence()
      .flatMap { javaFile ->
        if (javaFile.isFile) {
          sequenceOf(javaFile)
        } else {
          javaFile.walkBottomUp()
            .filter { it.isFile }
        }
      }
      .onEach { file ->
        if (file.extension == "aar") {

          val dir = file.unzippedAarDir()

          if (dir.list().isNullOrEmpty()) {
            AarExtractor().extract(file, dir.mkdirsInline())
          }
        }
      }
      .filter { it.extension == "jar" }
      .toSet()
      .plus(
        unzippedDir
          .walkBottomUp()
          .filter { it.isFile && it.extension == "jar" }
      )
      .sorted()
      .joinToString("\n")
      .also { txt ->
        classpathReportFile.asFile.get()
          .createSafely(txt)
      }
  }

  companion object {
    /**
     * Registers the task.
     *
     * @param project The Gradle project.
     * @param sourceSetName The source set name.
     * @return The task provider.
     */
    fun register(
      project: GradleProject,
      sourceSetName: SourceSetName
    ): TaskProvider<ModuleCheckDependencyResolutionTask> {
      val sourceSetCaps = sourceSetName.value.capitalize()
      return project.tasks.registerOnce<ModuleCheckDependencyResolutionTask>(
        "resolve${sourceSetCaps}NonProjectDependencies"
      ) { task ->

        task.classpathReportFile.set(Classpath.reportFile(project, sourceSetName))
        task.sourceSetNameProp.set(sourceSetName)
      }
    }
  }
}
