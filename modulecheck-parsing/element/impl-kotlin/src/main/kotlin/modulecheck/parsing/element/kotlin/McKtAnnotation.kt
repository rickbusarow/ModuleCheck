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
import modulecheck.parsing.psi.kotlinStdLibNameOrNull
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.internal.NameParser.NameParserPacket
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.lazy.lazyDeferred
import modulecheck.utils.mapToSet
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtValueArgument

internal fun KtAnnotated.annotations(
  parsingContext: ParsingContext,
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
  private val parsingContext: ParsingContext,
  override val psi: KtAnnotationEntry,
  override val parent: McKtElement
) : McKtAnnotation {

  override val referenceName = lazyDeferred {

    parsingContext.nameParser.parse(
      NameParserPacket(
        packageName = containingFile.packageName,
        imports = containingFile.imports.get().mapToSet { it.name },
        wildcardImports = containingFile.wildcardImports.get(),
        aliasedImports = containingFile.importAliases,
        resolved = emptySet(),
        unresolved = emptySet(),
        mustBeApi = emptySet(),
        apiReferenceNames = emptySet(),
        toExplicitReferenceName = ReferenceName::ExplicitKotlinReferenceName,
        toInterpretedReferenceName = ReferenceName::InterpretedKotlinReferenceName,
        stdLibNameOrNull = String::kotlinStdLibNameOrNull
      )
    )
      .resolved
      .singleOrNull { it.endsWith(psi.shortName!!.asString()) }
  }
}

data class RealMcKtAnnotationArgument(
  private val parsingContext: ParsingContext,
  override val psi: KtValueArgument,
  override val parent: McKtElement
) : McKtAnnotationArgument {

  override val value: Any = TODO()

  override val type: LazyDeferred<ReferenceName?> = lazyDeferred {

    psi
    TODO()
  }
}
