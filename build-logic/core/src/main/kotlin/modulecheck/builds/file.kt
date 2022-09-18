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

package builds

import java.io.File

/**
 * Walks upward in the file tree, looking for a directory which will resolve [relativePath].
 *
 * For example, given a receiver File path of './a/b/c/' and a `relativePath` of 'foo/bar.txt', this
 * function will attempt to resolve the following paths in order:
 * ```text
 * ./a/b/c/foo/bar.txt
 * ./a/b/foo/bar.txt
 * ./a/foo/bar.txt
 * ./foo/bar.txt
 * ```
 *
 * @returns the first path to contain an [existent][File.exists] File for [relativePath], or `null`
 *     if it could not be resolved
 * @see resolveInParent for a version which throws if nothing is resolved
 */
fun File.resolveInParentOrNull(relativePath: String): File? {
  return resolve(relativePath).existsOrNull()
    ?: parentFile?.resolveInParentOrNull(relativePath)
}

/**
 * Non-nullable version of [resolveInParentOrNull]
 *
 * @throws IllegalArgumentException if a file cannot be resolved
 * @see resolveInParentOrNull for a nullable, non-throwing variant
 */
fun File.resolveInParent(relativePath: String): File {
  return requireNotNull(resolveInParentOrNull(relativePath)) {
    "Could not resolve a file with relative path in any parent paths.\n" +
      "\t       relative path: $relativePath\n" +
      "\tstarting parent path: $absolutePath"
  }
}

fun File.existsOrNull(): File? = takeIf { it.exists() }

fun File.isDirectoryWithFiles(): Boolean = takeIf { it.isDirectory }
  ?.listFiles()
  ?.any { it.isDirectory } == true
