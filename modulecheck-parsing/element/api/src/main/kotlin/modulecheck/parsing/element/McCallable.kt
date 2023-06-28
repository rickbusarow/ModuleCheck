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

import modulecheck.name.TypeName
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

/** A sealed interface representing a callable element in the codebase. */
sealed interface McCallable :
  McElement,
  McElementWithParent<McElement>,
  HasVisibility,
  McAnnotated {

  /** A sealed interface representing a Java callable element. */
  sealed interface McJavaCallable : McCallable, McJavaElement {
    override val parent: McJavaElement
    override val visibility: McJavaVisibility
  }

  /** A sealed interface representing a Kotlin callable element. */
  sealed interface McKtCallable : McCallable, McKtElement {
    override val psi: KtCallableDeclaration
    override val parent: McKtElement
    override val visibility: McKtVisibility
  }
}

/** A sealed interface representing a property element in the codebase. */
sealed interface McProperty : McCallable, McElement, Declared {

  /** A deferred property representing the type name of the property. */
  val typeReferenceName: LazyDeferred<TypeName>

  /** Flag indicating whether the property is mutable. */
  val isMutable: Boolean

  /** A sealed interface representing a Java property element. */
  sealed interface McJavaProperty : McProperty, McJavaCallable {
    /** An interface representing a Java member property. */
    interface JavaMemberProperty : McJavaProperty
  }

  /** A sealed interface representing a Kotlin property element. */
  sealed interface McKtProperty : McProperty, McKtCallable {
    override val psi: KtCallableDeclaration

    /** A sealed interface representing a Kotlin member property element. */
    interface KtMemberProperty : McKtProperty {
      override val psi: KtProperty
    }

    /** A sealed interface representing a Kotlin extension property element. */
    interface KtExtensionProperty : KtMemberProperty, McHasTypeParameters {
      override val psi: KtProperty
    }

    /** A sealed interface representing a Kotlin constructor property element. */
    interface KtConstructorProperty : McKtProperty {
      override val psi: KtParameter
    }
  }
}

/** A sealed interface representing a parameter element in the codebase. */
sealed interface McParameter : McCallable, McElement {
  /** The index of the parameter. */
  val index: Int

  /** A sealed interface representing a Java parameter element. */
  interface McJavaParameter : McParameter, McJavaCallable {
    override val parent: McJavaElement
  }

  /** A sealed interface representing a Kotlin parameter element. */
  interface McKtParameter : McParameter, McKtCallable {
    override val parent: McKtElement
  }
}

/** A sealed interface representing a function element in the codebase. */
sealed interface McFunction : McCallable, McElement, McHasTypeParameters {

  /** A lazy set of parameters for the function. */
  val parameters: LazySet<McParameter>

  /** A lazy set of properties for the function. */
  val properties: LazySet<McProperty>

  /** A deferred property representing the return type of the function. */
  val returnType: LazyDeferred<ReferenceName>

  /** A lazy set of type parameters for the function. */
  val typeParamters: LazySet<McTypeParameter>

  /** A sealed interface representing a Java function element. */
  interface McJavaFunction : McFunction, McJavaCallable {
    override val parent: McJavaElement
  }

  /** represents a Kotlin function element. */
  interface McKtFunction : McFunction, McKtCallable {
    override val psi: KtFunction
    override val parent: McKtElement
    override val parameters: LazySet<McKtParameter>
    override val properties: LazySet<McKtProperty>
    override val returnType: LazyDeferred<ReferenceName>
    override val typeParamters: LazySet<McKtTypeParameter>
  }
}

/** A sealed interface representing an extension element in the codebase. */
sealed interface McExtensionElement : McKtCallable, McKtElement {
  /** The receiver type. */
  val receiver: McType

  /** A sealed interface representing a Kotlin extension property. */
  interface McKtExtensionProperty : McExtensionElement, McKtProperty

  /** A sealed interface representing a Kotlin extension function. */
  interface McKtExtensionFunction : McExtensionElement, McKtFunction
}
