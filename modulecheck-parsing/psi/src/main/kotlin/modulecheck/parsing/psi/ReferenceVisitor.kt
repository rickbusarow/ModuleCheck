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

import modulecheck.parsing.psi.internal.getChildrenOfTypeRecursive
import modulecheck.parsing.psi.internal.isPartOf
import modulecheck.parsing.psi.internal.isPrivateOrInternal
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.isFunctionalExpression

@Suppress("TooManyFunctions")
class ReferenceVisitor : KtTreeVisitorVoid() {

  val qualifiedExpressions: MutableSet<String> = mutableSetOf()
  val callableReferences: MutableSet<String> = mutableSetOf()
  val typeReferences: MutableSet<String> = mutableSetOf()

  val apiReferences: MutableSet<String> = mutableSetOf()

  override fun visitReferenceExpression(expression: KtReferenceExpression) {
    super.visitReferenceExpression(expression)

    typeReferences.add(expression.text)
  }

  override fun visitClassOrObject(classOrObject: KtClassOrObject) {
    super.visitClassOrObject(classOrObject)

    if (!classOrObject.isPrivateOrInternal()) {
      val superTypes = classOrObject.superTypeListEntries
        .flatMap { superTypeListEntry ->
          superTypeListEntry.getChildrenOfTypeRecursive<KtTypeReference>()
            .flatMap { it.getChildrenOfTypeRecursive<KtNameReferenceExpression>() }
            .map { it.text }
        }

      apiReferences.addAll(superTypes)
    }
  }

  override fun visitProperty(property: KtProperty) {
    super.visitProperty(property)
    if (!property.isPrivateOrInternal()) {

      val types = property.getChildrenOfTypeRecursive<KtNameReferenceExpression>()
        .map { it.text }

      apiReferences.addAll(types)
    }
  }

  override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
    super.visitPrimaryConstructor(constructor)
    if (!constructor.isPrivateOrInternal()) {
      parseValueParameters(constructor.valueParameters)
      parseTypeParameters(constructor.typeParameters)
    }
  }

  override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
    super.visitSecondaryConstructor(constructor)
    if (!constructor.isPrivateOrInternal()) {
      parseValueParameters(constructor.valueParameters)
      parseTypeParameters(constructor.typeParameters)
    }
  }

  private fun parseValueParameters(params: List<KtParameter>) {

    val typeRefStrings = params.mapNotNull { it.typeReference }
      .filterNot { it.isFunctionalExpression() }
      .flatMap { it.getChildrenOfTypeRecursive<KtNameReferenceExpression>() }
      .map { it.text }

    apiReferences.addAll(typeRefStrings)
  }

  private fun parseTypeParameters(params: List<KtTypeParameter>) {
    val typeParameterTypes = params
      .flatMap { it.getChildrenOfTypeRecursive<KtTypeReference>() }
      .flatMap { it.getChildrenOfTypeRecursive<KtNameReferenceExpression>() }
      .map { it.text }

    apiReferences.addAll(typeParameterTypes)
  }

  override fun visitNamedFunction(function: KtNamedFunction) {
    super.visitNamedFunction(function)
    if (!function.isPrivateOrInternal()) {
      parseValueParameters(function.valueParameters)
      parseTypeParameters(function.typeParameters)

      val types = function.typeReference // function.typeReference is the return type
        ?.getChildrenOfTypeRecursive<KtNameReferenceExpression>()
        .orEmpty()
        .plus(
          function.receiverTypeReference
            ?.getChildrenOfTypeRecursive<KtNameReferenceExpression>()
            .orEmpty()
        )
        .map { it.text }

      apiReferences.addAll(types)
    }
  }

  override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
    super.visitQualifiedExpression(expression)

    expression
      .takeIf { !it.isPartOf<KtImportDirective>() && !it.isPartOf<KtPackageDirective>() }
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
