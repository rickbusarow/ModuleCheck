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

package modulecheck.utils

import java.io.File
import java.nio.file.Path

/**
 * Checks if the receiver [File] exists and returns it, or null if it does not exist.
 *
 * @receiver [File] The file to check.
 * @return The file if it exists, null otherwise.
 */
fun File.existsOrNull(): File? = if (exists()) this else null

/**
 * vararg overload of [kotlin.io.resolve]
 *
 * @param relative The path segments of the child path.
 * @return A [File] representing the child path.
 */
fun File.resolve(vararg relative: String): File {
  return relative.fold(this) { parent, relativePath ->
    check(!relativePath.startsWith(File.separatorChar)) {
      "Do not include ${File.separatorChar} at the start of a relative path argument: $relativePath"
    }
    parent.resolve(relative = relativePath)
  }
}

/**
 * vararg overload of [java.nio.file.Path.resolve]
 *
 * @param relative The path segments of the child path.
 * @return A [File] representing the child path.
 */
fun Path.resolve(vararg relative: String): Path {
  return relative.fold(this) { parent, relativePath ->
    check(!relativePath.startsWith(File.separatorChar)) {
      "Do not include ${File.separatorChar} at the start of a relative path argument: $relativePath"
    }
    parent.resolve(relativePath)
  }
}

/**
 * Reads the receiver [File] and finds the minimum indent in its content.
 *
 * @receiver [File] The file to process.
 * @return A [String] containing the minimum indent.
 */
fun File.findMinimumIndent(): String {
  return readText().findMinimumIndent()
}

/**
 * Creates a new file if it doesn't already exist, creating parent
 * directories if necessary. If the file already exists, its content will
 * be overwritten. If content is provided, it will be written to the file.
 *
 * @param content The content to be written to the file. Defaults to null.
 * @param overwrite If true, any existing content will be overwritten. Otherwise, nothing is done.
 * @return The created file.
 */
fun File.createSafely(content: String? = null, overwrite: Boolean = true): File = apply {
  when {
    content != null && (!exists() || overwrite) -> makeParentDir().writeText(content)
    else -> {
      makeParentDir().createNewFile()
    }
  }
}

/**
 * Creates the directories represented by the receiver [File] if they don't already exist.
 *
 * @receiver [File] The directories to create.
 * @return The directory file.
 */
fun File.mkdirsInline(): File = apply(File::mkdirs)

/**
 * Creates the parent directory of the receiver [File] if it doesn't already exist.
 *
 * @receiver [File] The file whose parent directory is to be created.
 * @return The file with its parent directory created.
 */
fun File.makeParentDir(): File = apply {
  val fileParent = requireNotNull(parentFile) { "File's `parentFile` must not be null." }
  fileParent.mkdirs()
}

/**
 * Resolves the given child path against the receiver [File].
 *
 * @param child The child path to be resolved against the receiver [File].
 * @return The resultant [File].
 */
operator fun File.div(child: String): File = resolve(child)

/**
 * Splits the receiver [File]'s path into its individual segments.
 *
 * @receiver [File] The file to process.
 * @return A list of strings representing the segments of the file's path.
 */
fun File.segments(): List<String> = path.split(File.separatorChar)

/**
 * all parents starting from the direct parent. Does not include the receiver file.
 *
 * @receiver [File] The file whose parents to list.
 * @return A sequence of all parent directories of the receiver [File].
 */
fun File.parents(): Sequence<File> = generateSequence(parentFile) { it.parentFile }
