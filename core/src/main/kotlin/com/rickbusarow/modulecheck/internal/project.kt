/*
 * Copyright (C) 2020 Rick Busarow
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

package com.rickbusarow.modulecheck.internal

import com.rickbusarow.modulecheck.GradleProjectProxy
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

val GradleProjectProxy.srcRoot get() = File("$projectDir/src")
val GradleProjectProxy.mainJavaRoot get() = File("$srcRoot/main/java")
val GradleProjectProxy.androidTestJavaRoot get() = File("$srcRoot/androidTest/java")
val GradleProjectProxy.testJavaRoot get() = File("$srcRoot/test/java")
val GradleProjectProxy.mainKotlinRoot get() = File("$srcRoot/main/kotlin")
val GradleProjectProxy.androidTestKotlinRoot get() = File("$srcRoot/androidTest/kotlin")
val GradleProjectProxy.testKotlinRoot get() = File("$srcRoot/test/kotlin")

fun GradleProjectProxy.mainLayoutRootOrNull(): File? {
  val file = File("$srcRoot/main/res/layout")
  return if (file.exists()) file else null
}

fun GradleProjectProxy.mainResRootOrNull(): File? {
  val file = File("$srcRoot/main/res")
  return if (file.exists()) file else null
}

fun FileTreeWalk.dirs(): Sequence<File> = asSequence().filter { it.isDirectory }
fun FileTreeWalk.files(): Sequence<File> = asSequence().filter { it.isFile }

private fun KtFile.updateImports(
  oldImportsText: String?,
  newImportsText: String
): KtFile {
  val newText = when {
    oldImportsText != null -> text.replace(oldImportsText, newImportsText)
    // no existing imports -- add these under the package declaration
    packageDirective != null -> packageDirective!!.text + "\n\n" + newImportsText
    // no existing imports and file is at root of source directory
    else -> newImportsText + text
  }

  val path = absolutePath()

  return (
    psiFileFactory.createFileFromText(
      name,
      KotlinLanguage.INSTANCE,
      newText
    ) as KtFile
    ).apply {
      putUserData(ABSOLUTE_PATH, path)
    }
}

val File.generated get() = File(this.path + "/generated")

fun String.splitFirst(regex: Regex) = split(regex, 2)

@Suppress("SpreadOperator", "CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
fun String.splitFirst(
  vararg delimiters: String,
  ignoreCase: Boolean = false
) =
  split(delimiters = *delimiters, ignoreCase = ignoreCase, limit = 2)

fun createFile(
  path: String,
  text: String
) {
  File(path).writeText(text)
}
