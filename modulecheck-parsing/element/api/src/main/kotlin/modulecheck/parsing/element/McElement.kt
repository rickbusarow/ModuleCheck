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

package modulecheck.parsing.element

import modulecheck.parsing.element.McFile.McJavaFile
import modulecheck.parsing.element.McFile.McKtFile
import modulecheck.parsing.element.McType.McConcreteType
import modulecheck.parsing.element.McType.McConcreteType.McKtConcreteType
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.PackageName
import modulecheck.parsing.source.RawAnvilAnnotatedType
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.ReferenceName.ExplicitKotlinReferenceName
import modulecheck.parsing.source.ReferenceName.ExplicitReferenceName
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.LazySet.DataSource
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

interface Declared : HasVisibility {
  val declaredName: DeclaredName
  val packageName: PackageName
  val simpleName: String
}

sealed interface McElement {
  val psi: PsiElement
  val containingFile: McFile
}

sealed interface McJavaElement : McElement {
  override val containingFile: McJavaFile
}

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

sealed interface McElementWithParent<E : McElement> : McElement {
  val parent: E
}

fun McElementWithParent<*>.parents(): Sequence<McElement> {
  return generateSequence<McElement>(this) { element ->
    (element as? McElementWithParent<*>)?.parent
  }
}

interface McAnnotated {
  val annotations: LazySet<McAnnotation>
}

interface McAnnotation : McElement, McElementWithParent<McElement> {

  interface McKtAnnotation :
    McKtElement,
    McElementWithParent<McElement>,
    McAnnotation {
    override val parent: McKtElement
  }

  val referenceName: LazyDeferred<ReferenceName?>
}

interface McAnnotationArgument : McElement, McElementWithParent<McElement> {

  interface McKtAnnotationArgument :
    McKtElement,
    McElementWithParent<McElement>,
    McAnnotationArgument {
    override val parent: McKtElement
  }

  val type: LazyDeferred<ReferenceName?>
  val value: Any
}

sealed interface McFile : McElement, Declared {

  val javaFile: File
  val imports: DataSource<ExplicitReferenceName>

  val apiReferences: List<DataSource<ReferenceName>>
  val references: List<DataSource<ReferenceName>>

  val declarations: List<DataSource<DeclaredName>>

  val declaredTypes: LazySet<McConcreteType>
  val declaredTypesAndInnerTypes: LazySet<McConcreteType>

  interface McKtFile : McFile, McKtElement, McAnnotated {
    override val psi: KtFile

    override val declaredTypes: LazySet<McKtConcreteType>
    override val declaredTypesAndInnerTypes: LazySet<McKtConcreteType>

    val topLevelFunctions: LazySet<McFunction>
    val topLevelProperties: LazySet<McProperty>

    /** A weird, dated function for getting Anvil scope arguments */
    suspend fun getAnvilScopeArguments(
      allAnnotations: List<ExplicitReferenceName>,
      mergeAnnotations: List<ExplicitReferenceName>
    ): ScopeArgumentParseResult

    data class ScopeArgumentParseResult(
      val mergeArguments: Set<RawAnvilAnnotatedType>,
      val contributeArguments: Set<RawAnvilAnnotatedType>
    )

    val importAliases: Map<String, ExplicitKotlinReferenceName>
  }

  interface McJavaFile : McFile, McJavaElement

  val wildcardImports: DataSource<String>
}
