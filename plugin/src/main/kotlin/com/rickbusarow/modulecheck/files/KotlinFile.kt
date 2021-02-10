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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext

class KotlinFile(val ktFile: KtFile) : JvmFile() {

  override val name = ktFile.name

  override val packageFqName by lazy { ktFile.packageFqName.asString() }

  override val imports by lazy {

    usedImportsVisitor
      .usedImports()
      .mapNotNull { importDirective ->
        importDirective
          .importPath
          ?.pathStr
      }
      .toSet()
  }

  override val declarations by lazy {

    val v = DeclarationVisitor()
    ktFile.accept(v)
    v.declarations
  }

  override val wildcardImports by lazy {
    ktFile
      .importDirectives
      .mapNotNull { importDirective ->
        importDirective
          .importPath
          ?.pathStr
      }
      .filter { it.endsWith('*') }
      .toSet()
  }

  private val usedImportsVisitor by lazy {
    UsedImportsVisitor(BindingContext.EMPTY)
      .also { ktFile.accept(it) }
  }

  private val referenceVisitor by lazy {
    ReferenceVisitor()
      .also { ktFile.accept(it) }
  }

  private val namedReferences by lazy {

    ktFile.accept(usedImportsVisitor)
    usedImportsVisitor.namedReferences
      .map { it.text }
      .toSet()
  }

  private val typeReferences by lazy {
    referenceVisitor.typeReferences
      .mapNotNull { tr ->
        CHILD_PARAMETERS_REGEX.find(tr)?.value
      }
      .filterNot { it in imports }
      .toSet()
  }

  private val callableReferences by lazy {
    referenceVisitor.callableReferences
      .mapNotNull { tr ->
        CHILD_PARAMETERS_REGEX.find(tr)?.value
      }
      .toSet()
  }

  private val qualifiedExpressions by lazy {
    referenceVisitor.qualifiedExpressions
      .mapNotNull { tr ->
        CHILD_PARAMETERS_REGEX.find(tr)?.value
      }
      .toSet()
  }

  override val maybeExtraReferences by lazy {

    val allOther = typeReferences + callableReferences + qualifiedExpressions

    allOther + allOther.map {
      ktFile.packageFqName.asString() + "." + it
    } + wildcardImports.flatMap { wi ->

      allOther.map { tr ->
        wi.replace("*", tr)
      }
    }
      .toSet()
  }

  companion object {
    private val CHILD_PARAMETERS_REGEX = """^[a-zA-Z._`]*""".toRegex()
  }
}

class ReferenceVisitor : KtTreeVisitorVoid() {

  val qualifiedExpressions: MutableSet<String> = mutableSetOf()
  val callableReferences: MutableSet<String> = mutableSetOf()
  val typeReferences: MutableSet<String> = mutableSetOf()

  override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
    super.visitQualifiedExpression(expression)

    expression
      .takeIf { !it.isPartOf<KtImportDirective>() && !it.isPartOf<KtPackageDirective>() }
      // ?.takeIf { it.children.isEmpty() }
      ?.run { qualifiedExpressions.add(this.text) }
  }

  override fun visitTypeReference(typeReference: KtTypeReference) {
    super.visitTypeReference(typeReference)

    typeReferences.add(typeReference.text)
  }

  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)

    callableReferences.add(expression.text)
  }

  override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression) {
    super.visitCallableReferenceExpression(expression)

    callableReferences.add(expression.text)
  }
}

class DeclarationVisitor : KtTreeVisitorVoid() {

  val declarations: MutableSet<String> = mutableSetOf()

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
