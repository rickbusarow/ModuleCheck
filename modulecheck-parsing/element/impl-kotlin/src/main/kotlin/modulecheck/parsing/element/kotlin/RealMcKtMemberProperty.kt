/*
 * Copyright (C) 2021-2022 Rick Busarow
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

package modulecheck.parsing.element.kotlin

import modulecheck.parsing.element.HasKtVisibility
import modulecheck.parsing.element.McAnnotation
import modulecheck.parsing.element.McKtElement
import modulecheck.parsing.element.McProperty
import modulecheck.parsing.element.resolve.ParsingContext
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.PackageName
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.asExplicitKotlinReference
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.lazyDeferred
import modulecheck.utils.lazy.lazySet
import modulecheck.utils.requireNotNull
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty

data class RealMcKtMemberProperty(
  private val parsingContext: ParsingContext<PsiElement>,
  override val psi: KtProperty,
  override val parent: McKtElement
) : McProperty.McKtProperty.KtMemberProperty,
  HasKtVisibility by VisibilityDelegate(psi) {

  override val typeReferenceName: LazyDeferred<ReferenceName> = lazyDeferred {

    psi.delegateExpressionOrInitializer

    parsingContext.symbolResolver
      .declaredNameOrNull(psi.typeReference.requireNotNull())
      .requireNotNull()
      .name
      .asExplicitKotlinReference()
  }
  override val declaredName: DeclaredName
    get() = TODO("Not yet implemented")
  override val packageName: PackageName
    get() = containingFile.packageName
  override val simpleName: String
    get() = psi.name!!
  override val annotations: LazySet<McAnnotation> = lazySet {
    psi.annotations(parsingContext, parent = this)
  }
  override val isMutable: Boolean
    get() = psi.isVar
}

data class RealMcKtConstructorProperty(
  private val parsingContext: ParsingContext<PsiElement>,
  override val psi: KtParameter,
  override val parent: McKtElement
) : McProperty.McKtProperty.KtConstructorProperty,
  HasKtVisibility by VisibilityDelegate(psi) {

  override val typeReferenceName: LazyDeferred<ReferenceName> = lazyDeferred {

    parsingContext.symbolResolver
      .declaredNameOrNull(psi.typeReference.requireNotNull())
      .requireNotNull()
      .name
      .asExplicitKotlinReference()
  }

  override val declaredName: DeclaredName
    get() = TODO("Not yet implemented")
  override val packageName: PackageName
    get() = containingFile.packageName
  override val simpleName: String
    get() = psi.name!!
  override val annotations: LazySet<McAnnotation> = lazySet {
    psi.annotations(parsingContext, parent = this)
  }
  override val isMutable: Boolean
    get() = psi.isMutable
}
