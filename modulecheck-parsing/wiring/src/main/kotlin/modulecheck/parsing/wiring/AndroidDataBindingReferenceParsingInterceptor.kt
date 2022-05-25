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

package modulecheck.parsing.wiring

import kotlinx.coroutines.flow.firstOrNull
import modulecheck.parsing.source.AndroidDataBindingReference
import modulecheck.parsing.source.Reference
import modulecheck.parsing.source.internal.AndroidDataBindingNameProvider
import modulecheck.parsing.source.internal.NameParser
import modulecheck.parsing.source.internal.ParsingInterceptor
import modulecheck.utils.coroutines.any
import modulecheck.utils.mapToSet

class AndroidDataBindingReferenceParsingInterceptor(
  private val androidDataBindingNameProvider: AndroidDataBindingNameProvider
) : ParsingInterceptor {

  override suspend fun intercept(chain: ParsingInterceptor.Chain): NameParser.NameParserPacket {
    val packet = chain.packet

    val dataBindingReferences = mutableSetOf<Reference>()

    val stillUnresolved = packet.unresolved.toMutableSet()

    val dataBindingDeclarations = androidDataBindingNameProvider.get()

    if (dataBindingDeclarations.isNotEmpty()) {

      val concatenatedWildcards = packet.wildcardImports
        .flatMap { wildcardImport ->
          packet.unresolved
            .map { toResolve ->
              toResolve to AndroidDataBindingReference(wildcardImport.replace("*", toResolve))
            }
        }

      val fullyQualified = packet.unresolved
        .map { it to AndroidDataBindingReference(it) }

      val fromUnresolved = concatenatedWildcards
        .plus(fullyQualified)
        .mapNotNull { (toResolve, ref) ->
          dataBindingDeclarations.firstOrNull { ref.startsWith(it) }
            ?.let { declaration ->
              stillUnresolved.remove(toResolve)
              setOf(AndroidDataBindingReference(declaration.name), ref)
            }
        }
        .flatten()

      dataBindingReferences.addAll(fromUnresolved)

      val fromResolved = packet.resolved
        .map { it.name }
        .mapNotNull { resolved ->
          AndroidDataBindingReference(resolved)
            .takeIf { ref -> dataBindingDeclarations.any { ref.startsWith(it) } }
        }.toSet()
        .plus(fromUnresolved)

      dataBindingReferences.addAll(fromResolved)
    }

    val newResolved = dataBindingReferences + packet.resolved

    val newResolvedNames = newResolved.mapToSet { it.name }

    val new = packet.copy(
      resolved = newResolved,
      unresolved = stillUnresolved.filterNot { it in newResolvedNames }.toSet()
    )

    return chain.proceed(new)
  }
}
