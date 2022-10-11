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

package modulecheck.builds.dependencyGuardAggregate

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.util.prefixIfNot
import java.io.File

abstract class DependencyGuardAggregateTask : SourceTask() {

  @get:InputDirectory
  abstract val rootDir: RegularFileProperty

  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @TaskAction
  fun combine() {
    val rootFile = rootDir.asFile.get()

    val newText = source.files
      .map { file ->

        val relative = file.relativeTo(rootFile)

        val moduleDir = relative.parentFile.parentFile!!

        val modulePath = moduleDir.path.replace(File.separator, ":")
          .prefixIfNot(":")

        val classpath = file.nameWithoutExtension

        Triple(modulePath, classpath, file)
      }
      .joinToString("\n\n") { (modulePath, classpath, file) ->
        val prefix = "$modulePath -- $classpath"

        val lines = file.readText()
          .lines()
          .filterNot { it.isBlank() }

        file.deleteAndDeleteEmptyParents()

        lines.joinToString("\n") { "$prefix -- $it" }
      }

    outputFile.get().asFile.writeText(newText)
  }
}

abstract class DependencyGuardExplodeTask : DefaultTask() {

  @get:InputFile
  @get:PathSensitive(RELATIVE)
  abstract val aggregateFile: RegularFileProperty

  @get:Input
  abstract val rootDir: RegularFileProperty

  @TaskAction
  fun combine() {
    val agText = aggregateFile.asFile.get().readText()

    val split = agText.lines()
      .filterNot { it.isBlank() }
      .map { line ->
        line.split(" -- ")
          .map { segment -> segment.trim() }
          .let { (path, classpath, dependency) ->
            Triple(path, classpath, dependency)
          }
      }

    val pathToSplit = split.groupBy { (path, _, _) ->
      path
    }
      .mapValues { (_, triples) ->
        triples.map { it.second to it.third }
          .groupBy { it.first }
          .mapValues { (_, pairs) ->
            pairs.map { it.second }
          }
      }

    val rootFile = rootDir.get().asFile

    pathToSplit.forEach { (path, classpathToDepsMap) ->

      val relative = path.removePrefix(":")
        .replace(":", File.separator)

      val dependenciesDir = rootFile.resolve(relative).resolve("dependencies")
        .also { it.mkdirs() }

      classpathToDepsMap
        .forEach { (classpath, lines) ->
          val text = lines.joinToString("\n", postfix = "\n")

          dependenciesDir.resolve("$classpath.txt").writeText(text)
        }
    }
  }
}

fun File.deleteAndDeleteEmptyParents() {
  when {
    isFile -> delete()
    exists() && isDirectory && listFiles().isNullOrEmpty() -> delete()
    else -> return
  }

  parentFile?.deleteAndDeleteEmptyParents()
}
