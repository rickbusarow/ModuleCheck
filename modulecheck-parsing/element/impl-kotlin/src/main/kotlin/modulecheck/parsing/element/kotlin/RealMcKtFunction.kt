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

import modulecheck.parsing.element.Declared
import modulecheck.parsing.element.HasKtVisibility
import modulecheck.parsing.element.McAnnotation
import modulecheck.parsing.element.McFunction
import modulecheck.parsing.element.McKtDeclaredElement
import modulecheck.parsing.element.McParameter.McKtParameter
import modulecheck.parsing.element.McProperty.McKtProperty
import modulecheck.parsing.element.McType
import modulecheck.parsing.element.resolve.ParsingContext
import modulecheck.parsing.psi.internal.requireReferenceName
import modulecheck.parsing.source.ReferenceName
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.lazyDeferred
import modulecheck.utils.lazy.lazySet
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.resolve.BindingContext

data class RealMcKtFunction(
  override val parsingContext: ParsingContext<PsiElement>,
  override val psi: KtFunction,
  override val parent: McKtDeclaredElement
) : McFunction.McKtFunction,
  HasKtVisibility by VisibilityDelegate(psi),
  Declared by DeclaredDelegate(psi, parent),
  HasParsingContext {

  override val parameters: LazySet<McKtParameter> = lazySet { TODO("Not yet implemented") }
  override val properties: LazySet<McKtProperty> = lazySet { TODO("Not yet implemented") }
  override val returnType: LazyDeferred<ReferenceName> = lazyDeferred {
    bindingContext(BindingContext.FUNCTION, psi)
      ?.returnType
      .requireReferenceName()
  }
  override val typeParamters: LazySet<McType.McKtTypeParameter> =
    lazySet { TODO("Not yet implemented") }
  override val annotations: LazySet<McAnnotation> = lazySet { TODO("Not yet implemented") }
  override val typeParameters: LazySet<McType.McTypeParameter> =
    lazySet { TODO("Not yet implemented") }
}
