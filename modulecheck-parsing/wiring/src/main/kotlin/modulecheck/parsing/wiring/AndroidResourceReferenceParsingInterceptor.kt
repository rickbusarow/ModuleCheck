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

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toSet
import modulecheck.parsing.source.AndroidRDeclaredName
import modulecheck.parsing.source.AndroidRReference
import modulecheck.parsing.source.AndroidResourceReference
import modulecheck.parsing.source.QualifiedAndroidResourceReference
import modulecheck.parsing.source.Reference
import modulecheck.parsing.source.UnqualifiedAndroidResourceDeclaredName
import modulecheck.parsing.source.UnqualifiedAndroidResourceReference
import modulecheck.parsing.source.internal.AndroidRNameProvider
import modulecheck.parsing.source.internal.NameParser
import modulecheck.parsing.source.internal.NameParser.NameParserPacket
import modulecheck.parsing.source.internal.ParsingInterceptor
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.mapToSet

class AndroidResourceReferenceParsingInterceptor(
  private val androidRNameProvider: AndroidRNameProvider
) : ParsingInterceptor {

  override suspend fun intercept(chain: ParsingInterceptor.Chain): NameParser.NameParserPacket {
    val packet = chain.packet

    val rNames = androidRNameProvider.getAll()

    val localROrNull = packet.findLocalROrNull(rNames)

    val newFullyQualified = mutableSetOf<Reference>()

    val usedRDeclarations = rNames
      .filter { rName ->
        rName.isUsed(
          packet = packet,
          localROrNull = localROrNull
        )
      }
      .toSet()

    val resolvedRs: MutableSet<AndroidResourceReference> = usedRDeclarations
      .map { AndroidRReference(it.name) }
      .toMutableSet()

    val validRPrefixes = usedRDeclarations + listOfNotNull(localROrNull)

    val unqualifiedFromResolved = packet.resolved
      .mapNotNullTo(mutableSetOf()) { ref ->
        val unqualifiedRRef = validRPrefixes
          .firstOrNull { ref.startsWith(it) }
          ?.let { rPrefix ->

            if (rPrefix == localROrNull) {
              resolvedRs.add(AndroidRReference(rPrefix.name))
            }

            ref.name.removePrefix("${rPrefix.name}.")
              .twoPartUnqualifiedDeclarationOrNull()
              ?.let { unqualifiedDeclaredName ->

                val fqName = unqualifiedDeclaredName.toNamespacedDeclaredName(rPrefix).name

                newFullyQualified.add(QualifiedAndroidResourceReference(fqName))

                UnqualifiedAndroidResourceReference(unqualifiedDeclaredName.name)
              }
          }

        unqualifiedRRef
      }

    val stillUnresolved = mutableSetOf<String>()

    val unqualifiedFromUnresolved = packet.unresolved
      .mapNotNullTo(mutableSetOf()) { ref ->

        @Suppress("MagicNumber")
        val unqualifiedRRef = ref
          .threePartUnqualifiedDeclarationOrNull()
          ?.let { unqualifiedDeclaredName ->

            if (localROrNull != null) {
              val fqName = unqualifiedDeclaredName.toNamespacedDeclaredName(localROrNull).name

              newFullyQualified.add(QualifiedAndroidResourceReference(fqName))
            }

            UnqualifiedAndroidResourceReference(unqualifiedDeclaredName.name)
          }
          ?: ref.twoPartUnqualifiedDeclarationOrNull()
            ?.let { unqualifiedDeclaredName ->

              val rRef = packet.imports
                .firstNotNullOfOrNull { import ->
                  rNames.firstOrNull { rName ->
                    import.endsWith("${rName.name}.${unqualifiedDeclaredName.prefix}")
                  }
                }
                ?: packet.wildcardImports
                  .firstNotNullOfOrNull { wildcardImport ->
                    rNames.firstOrNull { rName ->
                      wildcardImport == "${rName.name}.*"
                    }
                  }

              if (rRef != null) {
                val fqName = unqualifiedDeclaredName.toNamespacedDeclaredName(rRef).name

                newFullyQualified.add(QualifiedAndroidResourceReference(fqName))
              }

              UnqualifiedAndroidResourceReference(unqualifiedDeclaredName.name)
            }

        if (unqualifiedRRef == null) {
          stillUnresolved.add(ref)
        }

        if (localROrNull != null && unqualifiedRRef != null) {
          resolvedRs.add(AndroidRReference(localROrNull.name))
        }

        unqualifiedRRef
      }

    val newResolved = resolvedRs
      .plus(newFullyQualified)
      .plus(unqualifiedFromResolved)
      .plus(unqualifiedFromUnresolved)
      .plus(packet.resolved)

    val newResolvedNames = newResolved.mapToSet { it.name }

    val new = packet.copy(
      resolved = newResolved,
      unresolved = stillUnresolved.filterNot { it in newResolvedNames }.toSet()
    )

    return chain.proceed(new)
  }

  private fun AndroidRDeclaredName.isUsed(
    packet: NameParserPacket,
    localROrNull: AndroidRDeclaredName?
  ): Boolean {
    val resolvedStrings = packet.resolved.mapToSet { it.name }

    return when {
      name in packet.imports -> true
      name in resolvedStrings -> true
      name in packet.unresolved -> true

      packet.imports.any { it.startsWith(name) } -> true
      resolvedStrings.any { it.startsWith(name) } -> true
      packet.unresolved.any { it.startsWith(name) } -> true

      localROrNull == null && packet.wildcardImports
        .any { wildcard ->
          wildcard.replace('*', 'R')
            .startsWith(name)
        } -> true

      else -> false
    }
  }

  private suspend fun NameParserPacket.findLocalROrNull(
    rNames: LazySet<AndroidRDeclaredName>
  ): AndroidRDeclaredName? {
    val aliasValueStrings = aliasedImports.values.mapToSet { it.name }
    return androidRNameProvider.getLocalOrNull()
      .takeIf { it?.name == "$packageName.R" }
      ?: rNames.firstOrNull { imports.contains(it.name) && !aliasValueStrings.contains(it.name) }
      ?: wildcardImports
        .firstNotNullOfOrNull { wildcard ->
          val synthetic = wildcard.replace('*', 'R')
          rNames.firstOrNull { it.name == synthetic }
        }
  }

  private fun String.twoPartUnqualifiedDeclarationOrNull(): UnqualifiedAndroidResourceDeclaredName? {
    return split('.')
      .takeIf { it.size == 2 }
      ?.let { (type, name) ->
        UnqualifiedAndroidResourceDeclaredName.fromValuePair(type, name)
      }
  }

  private fun String.threePartUnqualifiedDeclarationOrNull(): UnqualifiedAndroidResourceDeclaredName? {
    @Suppress("MagicNumber")
    return split('.')
      .takeIf { it.size == 3 }
      ?.let { (_, type, name) ->
        UnqualifiedAndroidResourceDeclaredName.fromValuePair(type, name)
      }
  }
}
