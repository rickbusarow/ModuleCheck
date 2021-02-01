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

package com.rickbusarow.modulecheck.files

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtStringTemplateEntry
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext

class KotlinFile(val ktFile: KtFile) : JvmFile() {
  override val packageFqName by lazy { ktFile.packageFqName.asString() }
  override val importDirectives by lazy {
    ktFile
      .importDirectives
      .mapNotNull { importDirective ->
        importDirective
          .importPath
          ?.pathStr
//            ?.split(".")
//            ?.dropLast(1)
//            ?.joinToString(".")
      }
      .toSet()
  }

  private val _declarations = mutableSetOf<String>()
  override val declarations: Set<String>
    get() = _declarations

  init {

    val v = DeclarationVisitor(_declarations)
    ktFile.accept(v)

    if (ktFile.name.endsWith("DaoModule.kt")) {

      val uiv = UsedImportsVisitor(BindingContext.EMPTY)

      ktFile.accept(uiv)
      val un = uiv.usedImports()

      if (un.isNotEmpty()) {
        println(
          """`````````````````````````````````````````````
          |
          |used --> ${un.joinToString("\n") { it.text }}
          |
          |
          |fqNames --> ${uiv.fqNames.joinToString("\n") { it.asString()  }}
          |
          |~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        """.trimMargin()
        )
      }
    }
  }
}

class DeclarationVisitor(val declarations: MutableSet<String>) : KtTreeVisitorVoid() {

  override fun visitNamedDeclaration(declaration: KtNamedDeclaration) {
    if (!declaration.isPrivateOrInternal()) {
      declaration.fqName?.let { declarations.add(it.asString()) }
    }

    super.visitNamedDeclaration(declaration)
  }
}

/**
 * Tests if this element is part of given PsiElement.
 */
inline fun <reified T : PsiElement> PsiElement.isPartOf() = getNonStrictParentOfType<T>() != null

/**
 * Tests if this element is part of a kotlin string.
 */
fun PsiElement.isPartOfString(): Boolean = isPartOf<KtStringTemplateEntry>()
