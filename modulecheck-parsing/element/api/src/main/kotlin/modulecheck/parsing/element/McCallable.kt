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

package modulecheck.parsing.element

import kotlinx.serialization.Serializable
import modulecheck.parsing.element.McCallable.McJavaCallable
import modulecheck.parsing.element.McCallable.McKtCallable
import modulecheck.parsing.element.McFunction.McKtFunction
import modulecheck.parsing.element.McParameter.McKtParameter
import modulecheck.parsing.element.McProperty.McKtProperty
import modulecheck.parsing.element.McType.McKtTypeParameter
import modulecheck.parsing.element.McType.McTypeParameter
import modulecheck.parsing.element.McVisibility.McJavaVisibility
import modulecheck.parsing.element.McVisibility.McKtVisibility
import modulecheck.parsing.source.ReferenceName
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.LazySet
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty

sealed interface McCallable :
  McElement,
  McElementWithParent<McElement>,
  HasVisibility,
  McAnnotated {

  sealed interface McJavaCallable : McCallable, McJavaElement {
    override val parent: McJavaElement
    override val visibility: McJavaVisibility
  }

  sealed interface McKtCallable : McCallable, McKtElement {
    override val psi: KtCallableDeclaration?
    override val parent: McKtElement
    override val visibility: McKtVisibility
  }
}

sealed interface McProperty : McCallable, McElement, Declared {

  val typeReferenceName: LazyDeferred<ReferenceName>

  val isMutable: Boolean

  sealed interface McJavaProperty : McProperty, McJavaCallable {
    interface JavaMemberProperty : McJavaProperty
  }

  sealed interface McKtProperty : McProperty, McKtCallable {
    override val psi: KtCallableDeclaration

    interface KtMemberProperty : McKtProperty {
      override val psi: KtProperty
    }

    interface KtExtensionProperty : KtMemberProperty, McHasTypeParameters {
      override val psi: KtProperty
    }

    interface KtConstructorProperty : McKtProperty {
      override val psi: KtParameter
    }
  }
}

@Serializable
sealed interface McParameter : McCallable, McElement {
  val index: Int

  interface McJavaParameter : McParameter, McJavaCallable {
    override val parent: McJavaElement
  }

  interface McKtParameter : McParameter, McKtCallable {
    override val parent: McKtElement
  }
}

sealed interface McFunction : McCallable, McElement, McHasTypeParameters {

  val parameters: LazySet<McParameter>
  val properties: LazySet<McProperty>

  val returnType: LazyDeferred<ReferenceName>
  val typeParamters: LazySet<McTypeParameter>

  interface McJavaFunction : McFunction, McJavaCallable {
    override val parent: McJavaElement
  }

  interface McKtFunction : McFunction, McKtCallable {
    override val psi: KtFunction?
    override val parent: McKtElement
    override val parameters: LazySet<McKtParameter>
    override val properties: LazySet<McKtProperty>

    override val returnType: LazyDeferred<ReferenceName>
    override val typeParamters: LazySet<McKtTypeParameter>
  }
}

sealed interface McExtensionElement : McKtCallable, McKtElement {
  val receiver: McType

  interface McKtExtensionProperty : McExtensionElement, McKtProperty
  interface McKtExtensionFunction : McExtensionElement, McKtFunction
}
