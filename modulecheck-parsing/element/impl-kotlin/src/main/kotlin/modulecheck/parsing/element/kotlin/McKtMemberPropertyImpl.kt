/*
 * Copyright (C) 2021-2023 Rick Busarow
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

import modulecheck.name.TypeName
import modulecheck.parsing.element.Declared
import modulecheck.parsing.element.HasKtVisibility
import modulecheck.parsing.element.McAnnotation
import modulecheck.parsing.element.McKtDeclaredElement
import modulecheck.parsing.element.McProperty
import modulecheck.parsing.element.resolve.McElementContext
import modulecheck.parsing.psi.internal.requireTypeName
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.lazyDeferred
import modulecheck.utils.lazy.lazySet
import modulecheck.utils.requireNotNull
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext

data class McKtMemberPropertyImpl(
  override val context: McElementContext<PsiElement>,
  override val psi: KtProperty,
  override val parent: McKtDeclaredElement
) : McProperty.McKtProperty.KtMemberProperty,
  HasKtVisibility by VisibilityDelegate(psi),
  Declared by DeclaredDelegate(psi, parent),
  HasMcElementContext {

  override val typeReferenceName: LazyDeferred<TypeName> = lazyDeferred {
    bindingContext(BindingContext.VARIABLE, psi)
      .requireNotNull()
      .type
      .requireTypeName()
  }

  override val annotations: LazySet<McAnnotation> = lazySet {
    psi.annotations(context, parent = this)
  }
  override val isMutable: Boolean
    get() = psi.isVar
}

data class McKtConstructorPropertyImpl(
  override val context: McElementContext<PsiElement>,
  override val psi: KtParameter,
  override val parent: McKtDeclaredElement
) : McProperty.McKtProperty.KtConstructorProperty,
  HasKtVisibility by VisibilityDelegate(psi),
  Declared by DeclaredDelegate(psi, parent),
  HasMcElementContext {

  override val typeReferenceName: LazyDeferred<TypeName> = lazyDeferred {
    bindingContext(BindingContext.VALUE_PARAMETER, psi)
      .requireNotNull()
      .type
      .requireTypeName()
  }

  override val annotations: LazySet<McAnnotation> = lazySet {
    psi.annotations(context, parent = this)
  }
  override val isMutable: Boolean
    get() = psi.isMutable
}