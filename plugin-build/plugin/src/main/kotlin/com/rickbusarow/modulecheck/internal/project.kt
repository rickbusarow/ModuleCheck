package com.rickbusarow.modulecheck.internal

import com.rickbusarow.modulecheck.internal.ABSOLUTE_PATH
import com.rickbusarow.modulecheck.internal.absolutePath
import org.gradle.api.Project
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import java.io.File


val Project.srcRoot get() = File("$projectDir/src")
val Project.mainJavaRoot get() = File("$srcRoot/main/java")
val Project.androidJavaTestRoot get() = File("$srcRoot/androidTest/java")
val Project.testJavaRoot get() = File("$srcRoot/test/java")

fun Project.mainLayoutRootOrNull(): File? {
  val file = File("$srcRoot/main/res/layout")
  return if (file.exists()) file else null
}

fun FileTreeWalk.dirs(): Sequence<File> = asSequence().filter { it.isDirectory }
fun FileTreeWalk.files(): Sequence<File> = asSequence().filter { it.isFile }

private fun KtFile.updateImports(
  oldImportsText: String?,
  newImportsText: String
): KtFile {
  val newText = when {
    oldImportsText != null   -> text.replace(oldImportsText, newImportsText)
    // no existing imports -- add these under the package declaration
    packageDirective != null -> packageDirective!!.text + "\n\n" + newImportsText
    // no existing imports and file is at root of source directory
    else                     -> newImportsText + text
  }

  val path = absolutePath()

  return (psiFileFactory.createFileFromText(name, KotlinLanguage.INSTANCE, newText) as KtFile).apply {
    putUserData(ABSOLUTE_PATH, path)
  }
}

val File.generated get() = File(this.path + "/generated")

fun String.splitFirst(regex: Regex) = split(regex, 2)

@Suppress("SpreadOperator")
fun String.splitFirst(vararg delimiters: String, ignoreCase: Boolean = false) =
  split(delimiters = *delimiters, ignoreCase = ignoreCase, limit = 2)

fun createFile(path: String, text: String) {
  File(path).writeText(text)
}
