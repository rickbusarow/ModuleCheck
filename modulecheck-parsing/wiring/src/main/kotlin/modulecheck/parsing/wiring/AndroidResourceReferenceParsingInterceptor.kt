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

package modulecheck.parsing.wiring

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toSet
import modulecheck.parsing.source.AndroidRDeclaredName
import modulecheck.parsing.source.AndroidRReferenceName
import modulecheck.parsing.source.QualifiedAndroidResourceReferenceName
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.UnqualifiedAndroidResource
import modulecheck.parsing.source.UnqualifiedAndroidResourceReferenceName
import modulecheck.parsing.source.append
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
    val language = packet.referenceLanguage

    val allAvailableRNames = androidRNameProvider.getAll()

    val localROrNull = packet.findLocalROrNull(allAvailableRNames)

    val newFullyQualified = mutableSetOf<ReferenceName>()

    val usedRDeclarations = allAvailableRNames
      .filter { rName ->
        rName.isUsed(
          packet = packet,
          localROrNull = localROrNull
        )
      }
      .toSet()

    val resolvedRs: MutableSet<AndroidRReferenceName> = allAvailableRNames.mapResolved(
      packet = packet,
      localROrNull = localROrNull
    ).toMutableSet()

    val validRPrefixes = usedRDeclarations + listOfNotNull(localROrNull)

    val unqualifiedFromResolved = packet.resolved
      .mapNotNullTo(mutableSetOf()) { ref ->
        val unqualifiedRRef = validRPrefixes
          .firstOrNull { ref.startsWith(it) }
          ?.let { rPrefix ->

            if (rPrefix == localROrNull) {
              resolvedRs.add(AndroidRReferenceName(localROrNull.packageName, language))
            }

            ref.name.removePrefix("${rPrefix.name}.")
              .twoPartUnqualifiedDeclarationOrNull()
              ?.let { unqualifiedDeclaredName ->

                val fqName = unqualifiedDeclaredName.toQualifiedDeclaredName(rPrefix).name

                newFullyQualified.add(
                  QualifiedAndroidResourceReferenceName(
                    name = fqName,
                    language = language
                  )
                )

                UnqualifiedAndroidResourceReferenceName(
                  unqualifiedDeclaredName.name,
                  language
                )
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
              val fqName = unqualifiedDeclaredName.toQualifiedDeclaredName(localROrNull).name

              newFullyQualified.add(
                QualifiedAndroidResourceReferenceName(
                  name = fqName,
                  language = packet.referenceLanguage
                )
              )
            }

            UnqualifiedAndroidResourceReferenceName(
              unqualifiedDeclaredName.name,
              language = packet.referenceLanguage
            )
          }
          ?: ref.twoPartUnqualifiedDeclarationOrNull()
            ?.let { unqualifiedDeclaredName ->

              val rRef = packet.imports
                .firstNotNullOfOrNull { import ->
                  allAvailableRNames.firstOrNull { rName ->
                    import.endsWith("${rName.name}.${unqualifiedDeclaredName.prefix}")
                  }
                }
                ?: packet.wildcardImports
                  .firstNotNullOfOrNull { wildcardImport ->
                    allAvailableRNames.firstOrNull { rName ->
                      wildcardImport == "${rName.name}.*"
                    }
                  }

              if (rRef != null) {
                val fqName = unqualifiedDeclaredName.toQualifiedDeclaredName(rRef).name

                newFullyQualified.add(
                  QualifiedAndroidResourceReferenceName(
                    fqName,
                    language = packet.referenceLanguage
                  )
                )
              }

              UnqualifiedAndroidResourceReferenceName(
                unqualifiedDeclaredName.name,
                language = packet.referenceLanguage
              )
            }

        if (unqualifiedRRef == null) {
          stillUnresolved.add(ref)
        }

        if (localROrNull != null && unqualifiedRRef != null) {

          val packageR = AndroidRReferenceName(
            packet.packageName,
            language = packet.referenceLanguage
          )
          if (allAvailableRNames.contains(packageR)) {
            resolvedRs.add(packageR)
          }
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

  private suspend fun LazySet<AndroidRDeclaredName>.mapResolved(
    packet: NameParserPacket,
    localROrNull: AndroidRDeclaredName?
  ): Set<AndroidRReferenceName> {
    val resolvedStrings = packet.resolved.mapToSet { it.name }

    val language = packet.referenceLanguage

    return mapNotNull { declaredName ->
      val name = declaredName.name

      when {
        name in packet.imports -> AndroidRReferenceName(declaredName.packageName, language)
        name in resolvedStrings -> AndroidRReferenceName(declaredName.packageName, language)
        name in packet.unresolved -> AndroidRReferenceName(declaredName.packageName, language)

        resolvedStrings
          .any { it.startsWith(name) } -> AndroidRReferenceName(declaredName.packageName, language)

        packet.unresolved
          .any { it.startsWith(name) } -> AndroidRReferenceName(declaredName.packageName, language)

        (localROrNull == null || localROrNull == declaredName) && packet.wildcardImports
          .any { wildcard ->
            wildcard.replace('*', 'R')
              .startsWith(name)
          } -> AndroidRReferenceName(declaredName.packageName, language)

        else -> null
      }
    }.toSet()
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
      .takeIf { it?.name == packageName.append("R") }
      ?: rNames.firstOrNull { imports.contains(it.name) && !aliasValueStrings.contains(it.name) }
      ?: wildcardImports
        .firstNotNullOfOrNull { wildcard ->
          val synthetic = wildcard.replace('*', 'R')
          rNames.firstOrNull { it.name == synthetic }
        }
  }

  private fun String.twoPartUnqualifiedDeclarationOrNull(): UnqualifiedAndroidResource? {
    return split('.')
      .takeIf { it.size == 2 }
      ?.let { (type, name) ->
        UnqualifiedAndroidResource.fromValuePair(type, name)
      }
  }

  private fun String.threePartUnqualifiedDeclarationOrNull(): UnqualifiedAndroidResource? {
    @Suppress("MagicNumber")
    return split('.')
      .takeIf { it.size == 3 }
      ?.let { (_, type, name) ->
        UnqualifiedAndroidResource.fromValuePair(type, name)
      }
  }
}
