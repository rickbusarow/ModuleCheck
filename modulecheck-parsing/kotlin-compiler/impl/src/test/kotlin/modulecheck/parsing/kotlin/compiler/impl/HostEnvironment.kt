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

package modulecheck.parsing.kotlin.compiler.impl

import io.github.classgraph.ClassGraph
import modulecheck.utils.requireNotNull
import java.io.File

/**
 * Utility object to provide everything we might discover from the host environment.
 *
 * @since 0.10.0
 */
@PublishedApi
internal object HostEnvironment {
  val classpath by lazy {
    getHostClasspaths()
  }

  val dispatchCore: File by lazy {
    findInClasspath(group = "com.rickbusarow.dispatch", module = "dispatch-core")
  }

  val dispatchTest: File by lazy {
    findInClasspath(group = "com.rickbusarow.dispatch", module = "dispatch-test")
  }

  val wireGrpcClient: File by lazy {
    findInClasspath(group = "com.squareup.wire", module = "wire-grpc-client-jvm")
  }

  val wireRuntime: File by lazy {
    findInClasspath(group = "com.squareup.wire", module = "wire-runtime-jvm")
  }

  val anvilAnnotations: File by lazy {
    findInClasspath(group = "com.squareup.anvil", module = "annotations")
  }

  val javaxInject: File by lazy {
    findInClasspath(group = "javax.inject", module = "javax.inject")
  }

  val okioJvm: File by lazy {
    findInClasspath(group = "com.squareup.okio", module = "okio-jvm")
  }

  val threeTenBP: File by lazy {
    findInClasspath(group = "org.threeten", module = "threetenbp")
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

  /**
   * Tries to find a file matching the given [regex] in the host process' classpath.
   *
   * @since 0.10.0
   */
  private fun findInClasspath(regex: Regex): File {
    return classpath.firstOrNull { classpath ->
      classpath.name.matches(regex)
    }
      .requireNotNull { "could not find classpath file via regex: $regex" }
  }

  /**
   * Tries to find a .jar file given pieces of its maven coordinates
   *
   * @since 0.10.0
   */
  @Suppress("MagicNumber")
  fun findInClasspath(
    group: String? = null,
    module: String? = null,
    version: String? = null
  ): File {
    require(group != null || module != null || version != null)
    return classpath.firstOrNull { classpath ->

      // example of a starting path:
      // [...]/com.square.anvil/compiler/1.0.0/911d07691411f7cbccf00d177ac41c1af38/compiler-1.0.0.jar
      val (fileGroup, fileModule, fileVersion) = classpath.absolutePath
        .split(File.separatorChar)
        // [..., "com.square.anvil", "compiler", "1.0.0", "91...38", "compiler-1.0.0.jar"]
        .dropLast(2)
        .takeLast(3)

      if (group != null && group != fileGroup) return@firstOrNull false
      if (module != null && module != fileModule) return@firstOrNull false
      version == null || version == fileVersion
    }
      .requireNotNull {
        "could not find classpath file [group: $group, module: $module, version: $version]"
      }
  }

  /**
   * Returns the files on the classloader's classpath and modulepath.
   *
   * @since 0.10.0
   */
  private fun getHostClasspaths(): List<File> {
    val classGraph = ClassGraph()
      .enableSystemJarsAndModules()
      .removeTemporaryFilesAfterScan()

    val classpaths = classGraph.classpathFiles
    val modules = classGraph.modules.mapNotNull { it.locationFile }

    return (classpaths + modules).distinctBy(File::getAbsolutePath)
  }
}
