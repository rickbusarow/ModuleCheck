@file:Suppress("TooManyFunctions")

package com.rickbusarow.modulecheck.internal

import org.jetbrains.kotlin.com.intellij.openapi.util.Key
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import java.io.File
import java.io.FileNotFoundException

val RELATIVE_PATH: Key<String> = Key("relativePath")
val ABSOLUTE_PATH: Key<String> = Key("absolutePath")

fun File.asKtFile(): KtFile =
  (psiFileFactory.createFileFromText(name, KotlinLanguage.INSTANCE, readText()) as? KtFile)?.apply {
    putUserData(ABSOLUTE_PATH, this@asKtFile.absolutePath)
  } ?: throw FileNotFoundException("could not find file $this")

fun KtFile.asFile(): File = File(absolutePath())

fun KtFile(absolutePath: String): KtFile = File(absolutePath).asKtFile()
fun KtFile(absoluteDirectory: String, fileName: String): KtFile = File(absoluteDirectory, fileName).asKtFile()

fun FileTreeWalk.ktFiles() = asSequence().filter { it.isFile }
  .mapNotNull { it.asKtFile() }

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

  return (psiFileFactory.createFileFromText(name, KotlinLanguage.INSTANCE, newText) as KtFile).apply {
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

  return (psiFileFactory.createFileFromText(name, KotlinLanguage.INSTANCE, newText) as KtFile).apply {
    putUserData(ABSOLUTE_PATH, path)
  }
}

fun KtFile.removeImport(fqImport: String): KtFile = removeImports(createImport(fqImport))
fun KtFile.removeImport(ktImportDirective: KtImportDirective): KtFile = removeImports(listOf(ktImportDirective))
fun KtFile.removeImports(vararg ktImportDirectives: KtImportDirective): KtFile = removeImports(ktImportDirectives.toList())
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
fun KtFile.addImport(ktImportDirective: KtImportDirective): KtFile = addImports(listOf(ktImportDirective))
fun KtFile.addImports(vararg ktImportDirectives: KtImportDirective): KtFile = addImports(ktImportDirectives.toList())
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

  return (psiFileFactory.createFileFromText(name, KotlinLanguage.INSTANCE, newText) as KtFile).apply {
    putUserData(ABSOLUTE_PATH, path)
  }
}

fun KtFile.replaceClass(oldClass: KtClass, newClass: KtClass): KtFile {

  val newText = text.replace(oldClass.text, newClass.text)

  val path = absolutePath()

  return (psiFileFactory.createFileFromText(name, KotlinLanguage.INSTANCE, newText) as KtFile).apply {
    putUserData(ABSOLUTE_PATH, path)
  }
}

fun KtFile.write(path: String = absolutePath()) {

  val javaFile = File(path)

  javaFile.mkdirs()
  javaFile.writeText(text)
}
