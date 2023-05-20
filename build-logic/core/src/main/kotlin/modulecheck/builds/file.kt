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

package modulecheck.builds

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Walks upward in the file tree, looking for a directory which will resolve [relativePath].
 *
 * For example, given a receiver File path of './a/b/c/' and a `relativePath` of
 * 'foo/bar.txt', this function will attempt to resolve the following paths in order:
 *
 * ```text
 * ./a/b/c/foo/bar.txt
 * ./a/b/foo/bar.txt
 * ./a/foo/bar.txt
 * ./foo/bar.txt
 * ```
 *
 * @returns the first path to contain an [existent][File.exists]
 *   File for [relativePath], or `null` if it could not be resolved
 * @see resolveInParent for a version which throws if nothing is resolved
 */
fun File.resolveInParentOrNull(relativePath: String): File? {
  return resolve(relativePath).existsOrNull()
    ?: parentFile?.resolveInParentOrNull(relativePath)
}

/**
 * Non-nullable version of [resolveInParentOrNull]
 *
 * @see resolveInParentOrNull for a nullable, non-throwing variant
 * @throws IllegalArgumentException if a file cannot be resolved
 */
fun File.resolveInParent(relativePath: String): File {
  return requireNotNull(resolveInParentOrNull(relativePath)) {
    "Could not resolve a file with relative path in any parent paths.\n" +
      "\t       relative path: $relativePath\n" +
      "\tstarting parent path: $absolutePath"
  }
}

fun File.existsOrNull(): File? = takeIf { it.exists() }

/**
 * @return true if the receiver [File] is a directory with
 *   at least one child file which satisfies [childPredicate]
 * @since 0.10.0
 */
fun File.isDirectoryWithFiles(
  childPredicate: (File) -> Boolean = { it.exists() }
): Boolean = !isFile && listFiles()?.any(childPredicate) == true

/**
 * Returns true if the receiver [File] is `/build/` or `/.gradle/`, but
 * there is no sibling `/build.gradle.kts` or `/settings.gradle.kts`.
 *
 * The most common cause of this would be switching between git branches
 * with different module structures. Since `build` and `.gradle` directories
 * are ignored in git, they'll stick around after a branch switch.
 *
 * @since 0.10.0
 */
fun File.isOrphanedBuildOrGradleDir(): Boolean {
  return when {
    !isDirectory -> false
    name != "build" && name != ".gradle" -> false
    !exists() -> false
    parentFile!!.hasGradleProjectFiles() -> false
    else -> true
  }
}

/**
 * Returns true if the receiver [File] is `/gradle.properties`, but
 * there is no sibling `/build.gradle.kts` or `/settings.gradle.kts`.
 *
 * The most common cause of this would be switching between git branches with
 * different module structures. Since all `gradle.properties` files except
 * the root are ignored in git, they'll stick around after a branch switch.
 *
 * @since 0.10.0
 */
fun File.isOrphanedGradleProperties(): Boolean {
  return when {
    !isFile -> false
    name != "gradle.properties" -> false
    parentFile!!.hasGradleProjectFiles() -> false
    else -> true
  }
}

/**
 * Returns true if the receiver [File] is a directory which contains at least one of
 * `settings.gradle.kts`, `settings.gradle`, `build.gradle.kts`, or `build.gradle`.
 *
 * @since 0.10.0
 */
fun File.hasGradleProjectFiles(): Boolean {
  return when {
    !isDirectory -> false
    resolve("settings.gradle.kts").exists() -> true
    resolve("settings.gradle").exists() -> true
    resolve("build.gradle.kts").exists() -> true
    resolve("build.gradle").exists() -> true
    else -> false
  }
}

/** Compares the contents of two zip files, ignoring metadata like timestamps. */
fun File.zipContentEquals(other: File): Boolean {

  require(extension == "zip") { "This file is not a zip file: file://$path" }
  require(other.extension == "zip") { "This file is not a zip file: file://$other" }

  fun ZipFile.getZipEntries(): Set<ZipEntry> {
    return entries()
      .asSequence()
      .filter { !it.isDirectory }
      .toHashSet()
  }

  return ZipFile(this).use { zip1 ->
    ZipFile(other).use use2@{ zip2 ->

      val zip1Entries = zip1.getZipEntries()
      val zip1Names = zip1Entries.mapTo(mutableSetOf()) { it.name }
      val zip2Entries = zip2.getZipEntries()
      val zip2Names = zip2Entries.mapTo(mutableSetOf()) { it.name }

      // Check if any file is contained in one archive but not the other
      if (zip1Names != zip2Names) {
        return@use false
      }

      // Check if the contents of any files with the same path are different
      for (file in zip1Names) {
        val zip1Entry = zip1.getEntry(file)
        val zip2Entry = zip2.getEntry(file)

        if (zip1Entry.size != zip2Entry.size) {
          return@use false
        }

        val inputStream1 = zip1.getInputStream(zip1Entry)
        val inputStream2 = zip2.getInputStream(zip2Entry)
        val content1 = inputStream1.readBytes()
        val content2 = inputStream2.readBytes()
        inputStream1.close()
        inputStream2.close()

        if (!content1.contentEquals(content2)) {
          return@use false
        }
      }
      return@use true
    }
  }
}
