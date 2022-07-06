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

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import modulecheck.parsing.source.AndroidDataBindingReferenceName
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.internal.AndroidDataBindingNameProvider
import modulecheck.utils.coroutines.any
import modulecheck.utils.mapToSet
import modulecheck.utils.singletonSet

class AndroidDataBindingReferenceParsingInterceptor2(
  private val androidDataBindingNameProvider: AndroidDataBindingNameProvider
) : ParsingInterceptor2 {

  override suspend fun intercept(chain: ParsingInterceptor2.Chain): ReferenceName? {
    val packet = chain.packet

    val dataBindingReferenceNames = mutableSetOf<ReferenceName>()

    val stillUnresolved = packet.toResolve.singletonSet().toMutableSet()

    val packageName = packet.file.packageName

    val dataBindingDeclarations = androidDataBindingNameProvider.get()

    if (dataBindingDeclarations.isNotEmpty()) {

      val concatenatedWildcards = packet.file.wildcardImports.get()
        .asSequence()
        .flatMap { wildcardImport ->
          stillUnresolved
            .map { toResolve ->
              toResolve to AndroidDataBindingReferenceName(
                name = wildcardImport.replace("*", toResolve.name),
                language = packet.referenceLanguage
              )
            }
        }

      val fullyQualified = packet.toResolve to AndroidDataBindingReferenceName(
        name = packet.toResolve.name,
        language = packet.referenceLanguage
      )

      val fromUnresolved = concatenatedWildcards
        .plus(fullyQualified)
        .asFlow()
        .mapNotNull { (toResolve, ref) ->
          dataBindingDeclarations.firstOrNull { ref.startsWith(it) }
            ?.let { declaration ->
              stillUnresolved.remove(toResolve)
              setOf(
                AndroidDataBindingReferenceName(
                  name = declaration.name,
                  language = packet.referenceLanguage
                ),
                ref
              )
            }
        }
        .toList()
        .flatten()

      dataBindingReferenceNames.addAll(fromUnresolved)

      val fromResolved = packet.resolved
        .map { it.name }
        .mapNotNull { resolved ->
          AndroidDataBindingReferenceName(
            name = resolved,
            language = packet.referenceLanguage
          )
            .takeIf { ref -> dataBindingDeclarations.any { ref.startsWith(it) } }
        }.toSet()
        .plus(fromUnresolved)

      dataBindingReferenceNames.addAll(fromResolved)
    }

    val newResolved = dataBindingReferenceNames + packet.resolved

    val newResolvedNames = newResolved.mapToSet { it.name }

    val new = packet.copy(
      resolved = newResolved,
      unresolved = stillUnresolved.filterNot { it in newResolvedNames }.toSet()
    )

    return chain.proceed(new)
  }
}
