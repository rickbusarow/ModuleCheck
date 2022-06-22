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

package modulecheck.parsing.psi

import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.internal.NameParser
import modulecheck.parsing.source.internal.ParsingInterceptor
import modulecheck.utils.mapToSet

class ConcatenatingParsingInterceptor : ParsingInterceptor {

  override suspend fun intercept(
    chain: ParsingInterceptor.Chain
  ): NameParser.NameParserPacket {

    val packet = chain.packet

    val resolved = packet.resolved
      .plus(packet.imports.mapToSet { packet.toExplicitReferenceName(it) })
      .toMutableSet()
    val resolvedApiReferenceNames = mutableSetOf<ReferenceName>()

    val stillUnresolved = packet.unresolved
      .filter { toResolve ->

        val referenceStart = toResolve.referenceFirstName()

        val concatOrNull = packet.imports
          .firstNotNullOfOrNull { import ->

            val matched = import.referenceLastName() == referenceStart
            when {
              matched && referenceStart == toResolve -> import
              matched -> {
                val withoutStart = toResolve.removePrefix(referenceStart)
                "$import$withoutStart"
              }
              else -> null
            }
          }
          ?: toResolve.inlineAliasOrNull(packet.aliasedImports)
          ?: packet.stdLibNameOrNull(toResolve)?.name

        if (concatOrNull != null) {

          val asReference = packet.toExplicitReferenceName(concatOrNull)

          if (packet.mustBeApi.contains(toResolve)) {
            resolvedApiReferenceNames.add(asReference)
          }

          resolved.add(asReference)
        }

        concatOrNull == null
      }

    val newPacket = packet.copy(
      resolved = resolved,
      unresolved = stillUnresolved.toSet(),
      apiReferenceNames = resolvedApiReferenceNames
    )

    return chain.proceed(newPacket)
  }

  private fun String.inlineAliasOrNull(
    aliasedImports: Map<String, ReferenceName.ExplicitReferenceName>
  ): String? {

    val toResolve = this
    // in `Lib1R.string.app_name`, firstName is `Lib1R`
    val firstName = toResolve.referenceFirstName()

    return aliasedImports[firstName]?.let { alias ->
      val newPrefix = alias.name
      val newSuffix = toResolve.removePrefix(firstName)
      "$newPrefix$newSuffix"
    }
  }

  private fun String.referenceFirstName(): String = split('.').first()
  private fun String.referenceLastName(): String = split('.').last()
}
