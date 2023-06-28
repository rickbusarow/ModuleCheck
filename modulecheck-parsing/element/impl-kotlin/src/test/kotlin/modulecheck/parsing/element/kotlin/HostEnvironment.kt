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

package modulecheck.parsing.element.kotlin

import io.github.classgraph.ClassGraph
import modulecheck.utils.requireNotNull
import java.io.File

/** Utility object to provide everything we might discover from the host environment. */
@PublishedApi
internal object HostEnvironment {
  val classpath by lazy {
    getHostClasspaths()
  }

  val anvilAnnotations: File by lazy {
    findInClasspath(group = "com.squareup.anvil", module = "annotations")
  }

  val javaxInject: File by lazy {
    findInClasspath(group = "javax.inject", module = "javax.inject")
  }

  val kotlinStdLibJar: File by lazy {
    findInClasspath(kotlinDependencyRegex("(kotlin-stdlib|kotlin-runtime)"))
  }

  val kotlinStdLibCommonJar: File by lazy {
    findInClasspath(kotlinDependencyRegex("kotlin-stdlib-common"))
  }

  val kotlinStdLibJdkJar: File by lazy {
    findInClasspath(kotlinDependencyRegex("kotlin-stdlib-jdk[0-9]+"))
  }

  private fun kotlinDependencyRegex(prefix: String): Regex {
    return Regex("$prefix(-[0-9]+\\.[0-9]+(\\.[0-9]+)?)([-0-9a-zA-Z]+)?\\.jar")
  }

  /** Tries to find a file matching the given [regex] in the host process' classpath. */
  private fun findInClasspath(regex: Regex): File {
    return classpath.firstOrNull { classpath ->
      classpath.name.matches(regex)
    }
      .requireNotNull { "could not find classpath file via regex: $regex" }
  }

  /** Tries to find a .jar file given pieces of its maven coordinates */
  private fun findInClasspath(
    group: String? = null,
    module: String? = null,
    version: String? = null
  ): File {
    require(group != null || module != null || version != null)
    return classpath.firstOrNull { classpath ->

      val classpathIsLocal = classpath.absolutePath.contains(".m2/repository/")

      val (fileGroup, fileModule, fileVersion) = if (classpathIsLocal) {
        parseMavenLocalClasspath(classpath)
      } else {
        parseGradleCacheClasspath(classpath)
      }

      if (group != null && group != fileGroup) return@firstOrNull false
      if (module != null && module != fileModule) return@firstOrNull false
      version == null || version == fileVersion
    }
      .requireNotNull {
        "could not find classpath file [group: $group, module: $module, version: $version]"
      }
  }

  private fun parseMavenLocalClasspath(classpath: File): List<String> {
    // ~/.m2/repository/com/square/anvil/compiler-utils/1.0.0/compiler-utils-1.0.0.jar
    return classpath.absolutePath
      .substringAfter(".m2/repository/")
      // Groups have their dots replaced with file separators, like "com/squareup/anvil".
      // Module names use dashes, so they're unchanged.
      .split(File.separatorChar)
      // ["com", "square", "anvil", "compiler-utils", "1.0.0", "compiler-1.0.0.jar"]
      // drop the simple name and extension
      .dropLast(1)
      .let { segments ->

        listOf(
          // everything but the last two segments is the group
          segments.dropLast(2).joinToString("."),
          // second-to-last segment is the module
          segments[segments.lastIndex - 1],
          // the last segment is the version
          segments.last()
        )
      }
  }

  @Suppress("MagicNumber")
  private fun parseGradleCacheClasspath(classpath: File): List<String> {
    // example of a starting path:
    // [...]/com.square.anvil/compiler/1.0.0/911d07691411f7cbccf00d177ac41c1af38/compiler-1.0.0.jar
    return classpath.absolutePath
      .split(File.separatorChar)
      // [..., "com.square.anvil", "compiler", "1.0.0", "91...38", "compiler-1.0.0.jar"]
      .dropLast(2)
      .takeLast(3)
  }

  /** Returns the files on the classloader's classpath and module path. */
  fun getHostClasspaths(): List<File> {
    val classGraph = ClassGraph()
      .enableSystemJarsAndModules()
      .removeTemporaryFilesAfterScan()

    val classpaths = classGraph.classpathFiles
    val modules = classGraph.modules.mapNotNull { it.locationFile }

    return (classpaths + modules).distinctBy(File::getAbsolutePath)
  }
}
