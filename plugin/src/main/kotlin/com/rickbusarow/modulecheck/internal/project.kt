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

package com.rickbusarow.modulecheck.internal

import com.android.build.gradle.TestedExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

val Project.srcRoot get() = File("$projectDir/src")
val Project.mainJavaRoot get() = File("$srcRoot/main/java")
val Project.androidTestJavaRoot get() = File("$srcRoot/androidTest/java")
val Project.testJavaRoot get() = File("$srcRoot/test/java")
val Project.mainKotlinRoot get() = File("$srcRoot/main/kotlin")
val Project.androidTestKotlinRoot get() = File("$srcRoot/androidTest/kotlin")
val Project.testKotlinRoot get() = File("$srcRoot/test/kotlin")

fun Project.mainLayoutRootOrNull(): File? {
  val file = File("$srcRoot/main/res/layout")
  return if (file.exists()) file else null
}

fun Project.mainResRootOrNull(): File? {
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
) = split(delimiters = delimiters, ignoreCase = ignoreCase, limit = 2)

fun createFile(
  path: String,
  text: String
) {
  File(path).writeText(text)
}

val Project.isAndroid: Boolean
  get() = extensions.findByType(TestedExtension::class) != null
