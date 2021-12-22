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

package modulecheck.parsing.psi

import kotlinx.coroutines.runBlocking
import modulecheck.parsing.psi.internal.getChildrenOfTypeRecursive
import modulecheck.parsing.psi.internal.hasAnnotation
import modulecheck.parsing.psi.internal.isPartOf
import modulecheck.parsing.psi.internal.isPrivateOrInternal
import modulecheck.parsing.source.KotlinFile
import modulecheck.utils.requireNotNull
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.isFunctionalExpression

@Suppress("TooManyFunctions")
class ReferenceVisitor(
  private val kotlinFile: KotlinFile
) : KtTreeVisitorVoid() {

  internal val qualifiedExpressions: MutableSet<PsiElement> = mutableSetOf()
  internal val callableReferences: MutableSet<PsiElement> = mutableSetOf()
  internal val typeReferences: MutableSet<PsiElement> = mutableSetOf()

  internal val apiReferences: MutableSet<PsiElement> = mutableSetOf()

  internal val constructorInjected: MutableSet<PsiElement> = mutableSetOf()
  internal val memberInjected: MutableSet<PsiElement> = mutableSetOf()

  internal val moduleBindingReferences: MutableSet<PsiElement> = mutableSetOf()

  internal val boundByInject: MutableSet<KtClassOrObject> = mutableSetOf()

  internal val inheritanceBoundTypes: MutableSet<PsiElement> = mutableSetOf()

  override fun visitReferenceExpression(expression: KtReferenceExpression) {
    super.visitReferenceExpression(expression)

    typeReferences += expression.parseTypeReferences()
  }

  override fun visitClassOrObject(classOrObject: KtClassOrObject) {
    super.visitClassOrObject(classOrObject)

    if (!classOrObject.isPrivateOrInternal()) {

      apiReferences += classOrObject.superTypeListEntries.parseTypeReferences()
    }
  }

  override fun visitProperty(property: KtProperty) {
    super.visitProperty(property)
    if (
      !property.isPrivateOrInternal() &&
      property.containingClass()?.isPrivateOrInternal() != true
    ) {

      val propertyType = property.getChildOfType<KtTypeReference>()
        ?: property.initializer?.getChildOfType<KtNameReferenceExpression>()

      if (propertyType != null) {
        apiReferences += propertyType

        if (property.hasAnnotation(kotlinFile, FqNames.inject)) {
          memberInjected += propertyType
        }
      }
    }
  }

  override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) = runBlocking {
    super.visitPrimaryConstructor(constructor)

    constructor.parseConstructor()
  }

  override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) = runBlocking {
    super.visitSecondaryConstructor(constructor)

    constructor.parseConstructor()
  }

  private fun <T : KtConstructor<T>> KtConstructor<T>.parseConstructor() {

    if (isPrivateOrInternal()) return

    val valueTypeRefs = valueParameters.parseTypeReferences()

    apiReferences += valueTypeRefs

    if (hasAnnotation(kotlinFile, FqNames.inject)) {

      constructorInjected += valueTypeRefs

      boundByInject += containingClassOrObject.requireNotNull {
        "unable to find a containing class for the constructor $text"
      }
    }

    apiReferences += typeParameters.parseTypeReferences()
  }

  override fun visitNamedFunction(function: KtNamedFunction) = runBlocking {
    super.visitNamedFunction(function)
    if (!function.isPrivateOrInternal()) {
      apiReferences += function.valueParameters.parseTypeReferences()
      apiReferences += function.typeParameters.parseTypeReferences()

      // function.typeReference is the return type
      apiReferences += function.typeReference?.parseTypeReferences().orEmpty()
      apiReferences += function.receiverTypeReference?.parseTypeReferences().orEmpty()
    }
  }

  private fun List<PsiElement>.parseTypeReferences(): Set<PsiElement> =
    flatMap { psiElement ->
      psiElement.parseTypeReferences()
    }
      .toSet()

  private fun PsiElement.parseTypeReferences(): Set<PsiElement> =
    listOfNotNull(
      when (this) {
        is KtTypeReference -> this
        is KtNameReferenceExpression -> this
        is KtDotQualifiedExpression -> this
        else -> null
      }
    )
      .plus(
        getChildrenOfTypeRecursive<KtTypeReference>()
          .filterNot { it.isFunctionalExpression() }
      )
      .plus(
        getChildrenOfTypeRecursive<KtNameReferenceExpression>()
          .filterNot { it.isFunctionalExpression() }
      )
      .plus(
        getChildrenOfTypeRecursive<KtDotQualifiedExpression>()
          .filterNot { it.isFunctionalExpression() }
      )
      .toSet()

  override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
    super.visitQualifiedExpression(expression)

    expression
      .takeIf { !it.isPartOf<KtImportDirective>() && !it.isPartOf<KtPackageDirective>() }
      ?.run {
        qualifiedExpressions += this.parseTypeReferences()
      }
  }

  override fun visitClassLiteralExpression(expression: KtClassLiteralExpression) {
    super.visitClassLiteralExpression(expression)

    typeReferences += expression
  }

  override fun visitTypeReference(typeReference: KtTypeReference) {
    super.visitTypeReference(typeReference)

    // typeReferences.add(typeReference.text)
    typeReferences += typeReference
  }

  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)

    // callableReferences.add(expression.text)
    callableReferences += expression.parseTypeReferences()
  }

  override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression) {
    super.visitCallableReferenceExpression(expression)

    // callableReferences.add(expression.text)
    callableReferences += expression.parseTypeReferences()
  }
}
