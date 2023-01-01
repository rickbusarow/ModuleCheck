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

import modulecheck.parsing.element.McAnnotation.McKtAnnotation
import modulecheck.parsing.element.McAnnotationArgument.McKtAnnotationArgument
import modulecheck.parsing.element.McKtElement
import modulecheck.parsing.element.resolve.NameParser2.NameParser2Packet
import modulecheck.parsing.element.resolve.ParsingContext
import modulecheck.parsing.psi.kotlinStdLibNameOrNull
import modulecheck.parsing.source.McName.CompatibleLanguage.KOTLIN
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.ReferenceName.Companion.asReferenceName
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.lazyDeferred
import modulecheck.utils.mapToSet
import modulecheck.utils.requireNotNull
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtValueArgument

internal fun KtAnnotated.annotations(
  parsingContext: ParsingContext<PsiElement>,
  parent: McKtElement
): Set<RealMcKtAnnotation> = annotationEntries
  .mapToSet {
    RealMcKtAnnotation(
      parsingContext = parsingContext,
      psi = it,
      parent = parent
    )
  }

data class RealMcKtAnnotation(
  private val parsingContext: ParsingContext<PsiElement>,
  override val psi: KtAnnotationEntry,
  override val parent: McKtElement
) : McKtAnnotation {

  override val referenceName = lazyDeferred {

    parsingContext.nameParser.parse(
      NameParser2Packet(
        file = containingFile,
        toResolve = psi.shortName!!.asString().asReferenceName(KOTLIN),
        referenceLanguage = KOTLIN,
        stdLibNameOrNull = { name.kotlinStdLibNameOrNull() }
      )
    )
      .requireNotNull()
  }
}

data class RealMcKtAnnotationArgument(
  private val parsingContext: ParsingContext<PsiElement>,
  override val psi: KtValueArgument,
  override val parent: McKtElement
) : McKtAnnotationArgument {

  override val value: Any = TODO()

  override val type: LazyDeferred<ReferenceName?> = lazyDeferred {

    psi
    TODO()
  }
}
