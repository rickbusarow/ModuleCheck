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

package com.rickbusarow.modulecheck.files

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.psi.psiUtil.isPublic

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
  }
}

class DeclarationVisitor(val declarations: MutableSet<String>) : KtTreeVisitorVoid() {

  override fun visitNamedDeclaration(declaration: KtNamedDeclaration) {

    if (!declaration.isPrivateOrInternal()) {
      declaration.fqName?.let { declarations.add(it.asString()) }
    }

    super.visitNamedDeclaration(declaration)
  }

  override fun visitReferenceExpression(expression: KtReferenceExpression) {
//    expression
//      .takeIf { !it.isPartOf<KtImportDirective>() && !it.isPartOf<KtPackageDirective>() }
//      ?.takeIf { it.children.isEmpty() }
//      ?.run { println(this.text) }

    super.visitReferenceExpression(expression)
  }
}

fun KtModifierListOwner.isPublicNotOverridden() = isPublic && !isOverride()
fun KtModifierListOwner.isPrivateOrInternal() = isPrivate() || isInternal()
fun KtModifierListOwner.isAbstract() = hasModifier(KtTokens.ABSTRACT_KEYWORD)
fun KtModifierListOwner.isOverride() = hasModifier(KtTokens.OVERRIDE_KEYWORD)
fun KtModifierListOwner.isOpen() = hasModifier(KtTokens.OPEN_KEYWORD)
fun KtModifierListOwner.isExternal() = hasModifier(KtTokens.EXTERNAL_KEYWORD)
fun KtModifierListOwner.isOperator() = hasModifier(KtTokens.OPERATOR_KEYWORD)
fun KtModifierListOwner.isConstant() = hasModifier(KtTokens.CONST_KEYWORD)
fun KtModifierListOwner.isInternal() = hasModifier(KtTokens.INTERNAL_KEYWORD)
fun KtModifierListOwner.isLateinit() = hasModifier(KtTokens.LATEINIT_KEYWORD)
fun KtModifierListOwner.isInline() = hasModifier(KtTokens.INLINE_KEYWORD)
fun KtModifierListOwner.isExpect() = hasModifier(KtTokens.EXPECT_KEYWORD)

/**
 * Tests if this element is part of given PsiElement.
 */
inline fun <reified T : PsiElement> PsiElement.isPartOf() = getNonStrictParentOfType<T>() != null

/**
 * Tests if this element is part of a kotlin string.
 */
fun PsiElement.isPartOfString(): Boolean = isPartOf<KtStringTemplateEntry>()
