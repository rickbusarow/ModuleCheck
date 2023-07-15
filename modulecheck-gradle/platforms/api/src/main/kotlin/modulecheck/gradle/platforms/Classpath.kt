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

@file:UseSerializers(FileAsStringSerializer::class)

package modulecheck.gradle.platforms

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import modulecheck.gradle.platforms.internal.GradleProject
import modulecheck.model.dependency.AndroidSdk.Companion.parseAndroidSdkJarFromPath
import modulecheck.model.dependency.Identifier
import modulecheck.model.dependency.MavenCoordinates.Companion.parseMavenCoordinatesFromGradleCache
import modulecheck.model.sourceset.SourceSetName
import modulecheck.project.McProject
import modulecheck.utils.requireExists
import modulecheck.utils.serialization.FileAsStringSerializer
import java.io.File

/**
 * Represents a classpath with a list of Maven coordinates and their corresponding files.
 *
 * @property mavenCoordinatesWithFiles The list of Maven coordinates and their corresponding files.
 */
@Serializable
data class Classpath(val mavenCoordinatesWithFiles: List<MavenCoordinatesWithFile>) {

  /** Returns the list of Maven coordinates. */
  fun coordinates() = mavenCoordinatesWithFiles.map { it.identifier }

  /** Returns the list of files. */
  fun files() = mavenCoordinatesWithFiles.map { it.file }

  companion object {

    /**
     * Returns the report file for the given project and source set name.
     *
     * @param project The project.
     * @param sourceSetName The source set name.
     * @return The report file.
     */
    fun reportFile(project: GradleProject, sourceSetName: SourceSetName): File {
      return project.buildDir.reportFile(sourceSetName)
    }

    /**
     * Returns the report file for the given project and source set name.
     *
     * @param project The project.
     * @param sourceSetName The source set name.
     * @return The report file.
     */
    fun reportFile(project: McProject, sourceSetName: SourceSetName): File {
      return project.projectDir.resolve("build").reportFile(sourceSetName)
    }

    private fun File.reportFile(sourceSetName: SourceSetName): File {
      return resolve("outputs/modulecheck/classpath/${sourceSetName.value}.txt")
    }

    /**
     * Returns a classpath from the report file of the given project and source set name.
     *
     * @param project The project.
     * @param sourceSetName The source set name.
     * @return The classpath.
     */
    fun from(project: GradleProject, sourceSetName: SourceSetName): Classpath {
      return reportFile(project, sourceSetName).parseToClasspath()
    }

    /**
     * Returns a classpath from the report file of the given project and source set name.
     *
     * @param project The project.
     * @param sourceSetName The source set name.
     * @return The classpath.
     */
    fun from(project: McProject, sourceSetName: SourceSetName): Classpath {
      return reportFile(project, sourceSetName).parseToClasspath()
    }

    private fun File.parseToClasspath(): Classpath {
      requireExists { "The expected classpath report file is missing at $absolutePath" }

      val extensions = setOf("jar", "aar")

      val coordinatesWithFiles = readText()
        .lineSequence()
        .filter { it.isNotBlank() }
        .map { it.trim() }
        // filter out .json and .txt files added by Android modules
        .filter { it.substringAfterLast(".") in extensions }
        .map { line ->

          val identifier = File(line).parseMavenCoordinatesFromGradleCache()
            ?: File(line).parseAndroidSdkJarFromPath()

          // TODO for special cases:
          //   handle BuildConfig.java and R.java files

          /*
          val file = when (identifier) {
            is MavenCoordinates -> {

              if (line.endsWith("aar")) {
                val subDir = when {
                  identifier.version.isNullOrBlank() -> identifier.moduleName
                  else -> identifier.moduleName + "-" + identifier.version.orEmpty()
                }

                resolveSibling("unzipped") / subDir / "jars" / "classes.jar"
              } else {
                File(line)
              }
            }

            is AndroidSdk -> File(line)
            null -> error("could not parse line - $line")
            else -> error("could not parse line - $line")
          }

          MavenCoordinatesWithFile(identifier, file)
           */

          MavenCoordinatesWithFile(identifier!!, File(line))
        }
        .toList()

      return Classpath(coordinatesWithFiles)
    }
  }

  /**
   * Represents a Maven coordinate with its corresponding file.
   *
   * @property identifier The Maven coordinate.
   * @property file The file.
   */
  @Serializable
  data class MavenCoordinatesWithFile(
    val identifier: Identifier,
    val file: File
  )
}
