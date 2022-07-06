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

package modulecheck.parsing.element.resolve

import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import modulecheck.parsing.gradle.model.SourceSetName
import modulecheck.parsing.psi.internal.DeclarationsInPackageProvider
import modulecheck.parsing.source.QualifiedDeclaredName
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.ReferenceName.Companion.asReferenceName

class ConcatenatingParsingInterceptor2(
  private val declarationsInPackageProvider: DeclarationsInPackageProvider,
  private val sourceSetName: SourceSetName
) : ParsingInterceptor2 {

  override suspend fun intercept(
    chain: ParsingInterceptor2.Chain
  ): ReferenceName? {

    val packet = chain.packet

    val packageName = packet.file.packageName

    val toResolve = packet.toResolve

    return packet.file.imports
      .get()
      .firstNotNullOfOrNull { importReference ->

        val matched = importReference.endsWith(toResolve.referenceFirstName())

        val referenceStart = toResolve.referenceFirstName()

        when {
          // Given a simple name without a qualifier and a matching import, like:
          // toResolve : "Foo"
          // import: "com.example.Foo"
          // ... just return the import.
          matched && referenceStart == toResolve.name -> importReference
          // If it's matched but the name to resolve is qualified, then remove the part that matched
          // and concatenate.
          // toResolve: Foo.Bar
          // import: com.example.Foo
          // withoutStart = .Bar
          // concatenated = com.example.Foo.Bar
          matched -> {
            val withoutStart = toResolve.segments.drop(1).joinToString(".")
            "$importReference.$withoutStart".asReferenceName(packet.referenceLanguage)
          }

          else -> null
        }
      }
      ?: packet.stdLibNameOrNull(toResolve)
      ?: declarationsInPackageProvider
        .getWithUpstream(
          sourceSetName = sourceSetName,
          packageName = packageName
        )
        .filterIsInstance<QualifiedDeclaredName>()
        .firstOrNull { it.isTopLevel && it.endsWithSimpleName(toResolve.referenceFirstName()) }
        ?.name
        ?.asReferenceName(packet.referenceLanguage)
      ?: chain.proceed(packet)
  }

  private fun String.referenceFirstName(): String = split('.').first()
  private fun String.referenceLastName(): String = split('.').last()

  private fun ReferenceName.referenceFirstName(): String = segments.first()
  private fun ReferenceName.referenceLastName(): String = segments.last()
}
