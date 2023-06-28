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

import modulecheck.parsing.element.McFile.McJavaFile
import modulecheck.parsing.element.McFile.McKtFile
import modulecheck.parsing.element.McFunction.McKtFunction
import modulecheck.parsing.element.McProperty.McKtProperty
import modulecheck.parsing.element.McType.McConcreteType.McJavaType
import modulecheck.parsing.element.McType.McConcreteType.McKtType
import modulecheck.utils.lazy.LazySet

sealed interface McType : McElementWithParent<McElement>, McAnnotated, McHasTypeParameters {

  /**
   * In a concrete type, this represents super-classes and interfaces.
   *
   * In a generic type, supers are the upper bound(s).
   */
  val superTypes: LazySet<McType>

  /** Represents a class, interface, object, or companion object */
  sealed interface McConcreteType : McType, Declared {

    override val containingFile: McFile

    val innerTypes: LazySet<McConcreteType>
    val innerTypesRecursive: LazySet<McType>
    val properties: LazySet<McProperty>
    val functions: LazySet<McFunction>

    interface McJavaType : McType, McJavaElement {
      override val parent: McJavaElement
    }

    interface McKtType : McType, McKtElement {
      override val parent: McKtElement
    }

    sealed interface McJavaConcreteType : McConcreteType, McJavaType, McJavaElement {
      override val innerTypes: LazySet<McJavaConcreteType>
      override val innerTypesRecursive: LazySet<McJavaConcreteType>

      override val containingFile: McJavaFile

      interface McJavaInterface : McJavaConcreteType, Declared
      interface McJavaClass : McJavaConcreteType, Declared {

        val constructors: LazySet<McFunction.McJavaFunction>
      }
    }

    interface McKtConcreteType :
      McKtType,
      McConcreteType,
      McKtDeclaredElement,
      McKtElement {
      override val parent: McKtElement
      override val innerTypes: LazySet<McKtConcreteType>
      override val innerTypesRecursive: LazySet<McKtConcreteType>

      override val containingFile: McKtFile
      override val properties: LazySet<McKtProperty>
      override val functions: LazySet<McKtFunction>

      interface McKtAnnotationClass : McKtConcreteType, McKtElement, Declared
      interface McKtClass : McKtConcreteType, McKtElement, Declared {

        val primaryConstructor: McFunction.McKtFunction?

        /** All constructors, including the primary if it exists */
        val constructors: LazySet<McFunction.McKtFunction>
      }

      interface McKtCompanionObject : McKtConcreteType, McKtElement, Declared
      interface McKtTypeAlias : McKtConcreteType, McKtElement, Declared
      interface McKtEnum : McKtConcreteType, McKtElement, Declared
      interface McKtInterface : McKtConcreteType, McKtElement, Declared
      interface McKtObject : McKtConcreteType, McKtElement, Declared
    }
  }

  /** Represents a generic type used as a parameter, like `<T>` or `<R: Any>`. */
  interface McTypeParameter : McType
  interface McJavaTypeParameter : McTypeParameter, McJavaType
  interface McKtTypeParameter : McTypeParameter, McKtType
}
