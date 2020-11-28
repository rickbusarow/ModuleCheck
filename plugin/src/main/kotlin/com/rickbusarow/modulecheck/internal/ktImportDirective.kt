package com.rickbusarow.modulecheck.internal

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.resolve.ImportPath

fun createImport(fqname: String) = psiElementFactory.createImportDirective(ImportPath(FqName(fqname), false))

fun List<KtImportDirective>.sortWeighted(): List<KtImportDirective> =
  sortedWith(compareBy({ it.isAlias }, { it.isJava }, { it.text }))

val KtImportDirective.isAlias: Boolean
  get() = (text.contains(" as "))

val KtImportDirective.isJava: Boolean
  get() = importedFqName?.asString()
    ?.let { it.startsWith("java") || it.startsWith("kotlin.") } ?: false
