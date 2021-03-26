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

package modulecheck.psi

import modulecheck.psi.internal.isPartOf
import modulecheck.psi.internal.isPrivateOrInternal
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isFunctionalExpression
import org.jetbrains.kotlin.resolve.BindingContext

@Suppress("TooManyFunctions")
class ReferenceVisitor(
  val bindingContext: BindingContext
) : KtTreeVisitorVoid() {

  val qualifiedExpressions: MutableSet<String> = mutableSetOf()
  val callableReferences: MutableSet<String> = mutableSetOf()
  val typeReferences: MutableSet<String> = mutableSetOf()

  val apiReferences: MutableSet<String> = mutableSetOf()

  override fun visitClassOrObject(classOrObject: KtClassOrObject) {
    super.visitClassOrObject(classOrObject)

    if (!classOrObject.isPrivateOrInternal()) {
      val superTypes = classOrObject.superTypeListEntries
        .mapNotNull {
          it
            .typeReference
            ?.text
            ?.removeGenericsAndSpecials()
        }

      apiReferences.addAll(superTypes)
    }
  }

  override fun visitProperty(property: KtProperty) {
    super.visitProperty(property)
    if (!property.isPrivateOrInternal()) {
      val type = property
        .typeReference
        ?.text
        ?.removeGenericsAndSpecials()

      if (type != null) {
        apiReferences.add(type)
      }
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

  fun parseValueParameters(params: List<KtParameter>) {
    val valueTypes = params
      .mapNotNull {
        it.typeReference
      }.filterNot { it.isFunctionalExpression() }
      .mapNotNull {
        it.typeElement
          ?.text
          ?.removeGenericsAndSpecials()
      }

    apiReferences.addAll(valueTypes)
  }

  fun parseTypeParameters(params: List<KtTypeParameter>) {
    val typeParameterTypes = params
      .mapNotNull {
        it.extendsBound
          ?.typeElement
          ?.text
          ?.removeGenericsAndSpecials()
      }

    apiReferences.addAll(typeParameterTypes)
  }

  override fun visitNamedFunction(function: KtNamedFunction) {
    super.visitNamedFunction(function)
    if (!function.isPrivateOrInternal()) {
      parseValueParameters(function.valueParameters)
      parseTypeParameters(function.typeParameters)
    }
  }

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

  fun String.removeGenericsAndSpecials() = replace("<[a-zA-Z?]>".toRegex(), "")
    .replace("[^a-zA-Z.]".toRegex(), "")
}
