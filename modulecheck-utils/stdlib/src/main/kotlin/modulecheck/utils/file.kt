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

fun File.existsOrNull(): File? = if (exists()) this else null

fun File.child(vararg childPath: String): File {
  return File(this, childPath.joinToString(File.separator))
}

fun Path.child(vararg childPath: String): File {
  return File(toFile(), childPath.joinToString(File.separator))
}

fun File.findMinimumIndent(): String {
  return readText().findMinimumIndent()
}

fun File.createSafely(content: String? = null): File = apply {
  toPath().parent?.toFile()?.mkdirs()
  if (content != null) {
    writeText(content)
  } else {
    createNewFile()
  }
}

/**
 * Creates the directories if they don't already exist
 *
 * @see File.mkdirs
 * @since 0.12.0
 */
fun File.mkdirsInline(): File = apply { mkdirs() }
