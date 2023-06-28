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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import modulecheck.parsing.element.McFile.McJavaFile
import modulecheck.parsing.element.McFile.McKtFile
import modulecheck.parsing.element.McType.McConcreteType
import modulecheck.parsing.element.McType.McConcreteType.McKtConcreteType
import modulecheck.parsing.element.McType.McTypeParameter
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.HasPackageName
import modulecheck.parsing.source.HasSimpleNames
import modulecheck.parsing.source.RawAnvilAnnotatedType
import modulecheck.parsing.source.ReferenceName
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.LazySet.DataSource
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

/** An element that has been declared, it can be a class, function, variable, etc. */
interface Declared :
  HasPackageName,
  HasSimpleNames {
  /** The name of this declared element. */
  val declaredName: DeclaredName

  /** A boolean indicating if this is an API element or not. */
  val isApi: Boolean get() = false
}

/**
 * Base interface for all code elements parsed from source
 * files. This includes classes, functions, variables, etc.
 */
sealed interface McElement {

  /** The PSI element representing the physical code element in the source code. */
  val psi: PsiElement

  /** The file that contains this element. */
  val containingFile: McFile

  /** The children elements of this element. */
  val children: Flow<McElement> get() = flowOf()
}

/** Represents a Java element in the source code. */
sealed interface McJavaElement : McElement {
  /** The Java file that contains this element. */
  override val containingFile: McJavaFile
}

/** Represents a Kotlin element in the source code. */
sealed interface McKtElement : McElement {

  override val psi: KtElement

  override val containingFile: McFile.McKtFile
    get() = when (this) {

      is McElementWithParent<*> -> (parent as McKtElement).containingFile

      is McKtFile -> this

      else -> throw IllegalArgumentException(

        "How did you call `containingFile` without being `McElementWithParent` or `McKtFile`?"

      )
    }
}

/** Represents a declared Kotlin element in the source code. */
sealed interface McKtDeclaredElement : McKtElement, Declared

/** Represents an element with a parent element. */
sealed interface McElementWithParent<E : McElement> : McElement {
  /** The parent element */
  val parent: E
}

/**
 * Generates a sequence of parent elements.
 *
 * @receiver An element with a parent.
 * @return A sequence of parent elements.
 */
fun McElementWithParent<*>.parents(): Sequence<McElement> {

  return generateSequence<McElement>(this) { element ->

    (element as? McElementWithParent<*>)?.parent
  }
}

/** Represents an annotated element. */
interface McAnnotated {

  /** The annotations of this element. */
  val annotations: LazySet<McAnnotation>
}

/** Represents an element with type parameters. */
interface McHasTypeParameters {

  /** The type parameters of this element. */
  val typeParameters: LazySet<McTypeParameter>
}

/** Represents an annotation. */
interface McAnnotation : McElement, McElementWithParent<McElement> {

  /** The reference name of this annotation. */
  val referenceName: LazyDeferred<ReferenceName?>

  interface McKtAnnotation :

    McKtElement,

    McElementWithParent<McElement>,

    McAnnotation {

    override val parent: McKtElement
  }
}

/** Represents an argument of an annotation. */
interface McAnnotationArgument : McElement, McElementWithParent<McElement> {

  interface McKtAnnotationArgument :
    McKtElement,
    McElementWithParent<McElement>,
    McAnnotationArgument {

    override val parent: McKtElement
  }

  /** The type of this argument. */
  val type: LazyDeferred<ReferenceName?>

  /** The value of this argument. */
  val value: Any
}

/** Represents a file. */
sealed interface McFile : McElement, HasPackageName {
  /** The actual file. */
  val file: File

  /** The imports in this file. */
  val imports: DataSource<ReferenceName>

  /** The API references in this file. */
  val apiReferences: List<DataSource<ReferenceName>>

  /** The references in this file. */
  val references: List<DataSource<ReferenceName>>

  /** The declarations in this file. */
  val declarations: List<DataSource<DeclaredName>>

  /** The declared types in this file. */
  val declaredTypes: LazySet<McConcreteType>

  /** The declared types and inner types in this file. */
  val declaredTypesAndInnerTypes: LazySet<McConcreteType>

  /** Represents a single Kotlin file. */
  interface McKtFile :
    McFile,
    McKtElement,
    McAnnotated {

    override val psi: KtFile

    override val declaredTypes: LazySet<McKtConcreteType>

    override val declaredTypesAndInnerTypes: LazySet<McKtConcreteType>

    /** The top level functions in this file. */
    val topLevelFunctions: LazySet<McFunction>

    /** The top level properties in this file. */
    val topLevelProperties: LazySet<McProperty>

    /** The import aliases in this file. */
    val importAliases: Map<String, ReferenceName>

    /** A weird, dated function for getting Anvil scope arguments */
    suspend fun getAnvilScopeArguments(
      allAnnotations: List<ReferenceName>,
      mergeAnnotations: List<ReferenceName>
    ): ScopeArgumentParseResult

    /**
     * Represents the parsed results for Anvil scope arguments.
     *
     * @property mergeArguments The set of merge arguments derived from Anvil annotations.
     * @property contributeArguments The set of contribute arguments derived from Anvil annotations.
     */
    data class ScopeArgumentParseResult(
      val mergeArguments: Set<RawAnvilAnnotatedType>,
      val contributeArguments: Set<RawAnvilAnnotatedType>
    )
  }

  /** Represents a Java file. */
  interface McJavaFile : McFile, McJavaElement

  /** The wildcard imports in this file. */
  val wildcardImports: DataSource<String>
}
