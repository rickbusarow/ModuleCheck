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

@file:Suppress("TooManyFunctions")

package modulecheck.psi.internal

import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.incremental.isKotlinFile
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import java.io.File
import java.io.FileNotFoundException

fun Sequence<File>.ktFiles() = asSequence().filter { it.isFile }
  .mapNotNull { it.asKtFile() }

fun Collection<File>.ktFiles() = asSequence()
  .filter { it.isKotlinFile(listOf("kt")) }
  .mapNotNull { it.asKtFile() }
  .toList()

val RELATIVE_PATH: Key<String> = Key("relativePath")
val ABSOLUTE_PATH: Key<String> = Key("absolutePath")

fun File.asKtsFileOrNull(): KtFile? =
  if (exists() && isKotlinFile(listOf("kts"))) asKtFile() else null

fun File.asKtFileOrNull(): KtFile? =
  if (exists() && isKotlinFile(listOf("kt"))) asKtFile() else null

fun File.asKtFile(): KtFile =
  (psiFileFactory.createFileFromText(name, KotlinLanguage.INSTANCE, readText()) as? KtFile)?.apply {
    putUserData(ABSOLUTE_PATH, this@asKtFile.absolutePath)
  } ?: throw FileNotFoundException("could not find file $this")

fun KtFile.asFile(): File = File(absolutePath())

fun KtFile(absolutePath: String): KtFile = File(absolutePath).asKtFile()
fun KtFile(
  absoluteDirectory: String,
  fileName: String
): KtFile =
  File(absoluteDirectory, fileName).asKtFile()
fun KtFile.absolutePath(): String =
  getUserData(ABSOLUTE_PATH) ?: error("KtFile '$name' expected to have an absolute path.")

fun KtFile.absolutePathDirectory(): String = absolutePath().removeSuffix(virtualFilePath)

fun KtFile.relativePath(): String = getUserData(RELATIVE_PATH)
  ?: error("KtFile '$name' expected to have an relative path.")

fun KtFile.replaceImports(newImportsText: String): KtFile {
  val oldImports = importList?.text

  val newText = when {
    oldImports != null -> text.replace(oldImports, newImportsText)
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

fun KtFile.sortImports(): KtFile {
  val oldImports = importDirectives

  val oldImportsText = importList?.text
  val newImportsText = oldImports.sortWeighted()
    .distinctBy { it.text }
    .joinToString("\n") { it.text }

  val path = absolutePath()

  val newText = when {
    oldImportsText != null -> text.replace(oldImportsText, newImportsText)
    // no existing imports -- add these under the package declaration
    packageDirective != null -> packageDirective!!.text + "\n\n" + newImportsText
    // no existing imports and file is at root of source directory
    else -> newImportsText + text
  }

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

fun KtFile.removeImport(fqImport: String): KtFile = removeImports(createImport(fqImport))
fun KtFile.removeImport(ktImportDirective: KtImportDirective): KtFile =
  removeImports(listOf(ktImportDirective))

fun KtFile.removeImports(
  vararg ktImportDirectives: KtImportDirective
): KtFile = removeImports(ktImportDirectives.toList())

fun KtFile.removeImports(ktImportDirectives: List<KtImportDirective>): KtFile {
  val oldImports = importDirectives

  val newImports = oldImports.filterNot {
    ktImportDirectives.map { it.text }
      .contains(it.text)
  }

  val oldImportsText = importList?.text
  val newImportsText = newImports.sortWeighted()
    .joinToString("\n") { it.text }

  return updateImports(oldImportsText, newImportsText)
}

fun KtFile.addImport(fqImport: String): KtFile = addImports(createImport(fqImport))
fun KtFile.addImport(ktImportDirective: KtImportDirective): KtFile =
  addImports(listOf(ktImportDirective))

fun KtFile.addImports(vararg ktImportDirectives: KtImportDirective): KtFile =
  addImports(ktImportDirectives.toList())

fun KtFile.addImports(ktImportDirectives: List<KtImportDirective>): KtFile {
  val oldImports = importDirectives

  val newImports = ktImportDirectives.toList()

  val oldImportsText = importList?.text
  val newImportsText = (oldImports + newImports).sortWeighted()
    .joinToString("\n") { it.text }

  return updateImports(oldImportsText, newImportsText)
}

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

fun KtFile.replaceClass(
  oldClass: KtClass,
  newClass: KtClass
): KtFile {
  val newText = text.replace(oldClass.text, newClass.text)

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

fun KtFile.write(path: String = absolutePath()) {
  val javaFile = File(path)

  javaFile.mkdirs()
  javaFile.writeText(text)
}
