/*
 * Copyright (C) 2021 Rick Busarow
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

package modulecheck.api.test

import org.intellij.lang.annotations.Language
import java.io.File

fun File.createSafely(content: String? = null) = apply {
  toPath().parent.toFile().mkdirs()
  if (content != null) {
    writeText(content)
  } else {
    createNewFile()
  }
}

fun File.writeKotlin(
  // @Language("kotlin")
  content: String
) {
  check(extension in listOf("kts", "kt")) {
    "Writing Kotlin to a non-Kotlin file with extension `$extension`."
  }
  createSafely(content.trimIndent())
}

fun File.writeGroovy(
  @Language("groovy")
  content: String
) {
  check(extension in listOf("groovy", "gradle")) {
    "Writing Groovy to a non-Groovy file with extension `$extension`."
  }
  createSafely(content.trimIndent())
}

fun File.writeJava(
  @Language("java")
  content: String
) {
  check(extension == "java") {
    "Writing Java to a non-Java file with extension `$extension`."
  }
  createSafely(content.trimIndent())
}
