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

package modulecheck.parsing.source.internal

import modulecheck.parsing.source.Reference
import modulecheck.utils.mapToSet

class InterpretingInterceptor : ParsingInterceptor {
  override suspend fun intercept(chain: ParsingInterceptor.Chain): NameParser.NameParserPacket {

    val packet = chain.packet

    val trimmedWildcards = packet.wildcardImports.map { it.removeSuffix(".*") }
    val newResolved = mutableSetOf<Reference>()
    val newApi = mutableSetOf<Reference>()

    chain.packet.unresolved
      .forEach { toResolve ->

        val interpreted = buildSet {
          // no import
          add(packet.toInterpretedReference(toResolve))

          // concat with package
          add(packet.toInterpretedReference("${packet.packageName}.$toResolve"))

          // concat with any wildcard imports
          addAll(trimmedWildcards.mapToSet { packet.toInterpretedReference("$it.$toResolve") })
        }

        newResolved.addAll(interpreted)
        if (packet.mustBeApi.contains(toResolve)) {
          newApi.addAll(interpreted)
        }
      }

    return packet.copy(
      resolved = packet.resolved + newResolved,
      apiReferences = packet.apiReferences + newApi,
      unresolved = emptySet()
    )
  }
}
