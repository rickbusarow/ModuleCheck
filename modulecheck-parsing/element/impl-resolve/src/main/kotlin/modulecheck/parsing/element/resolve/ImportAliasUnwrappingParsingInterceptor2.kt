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

package modulecheck.parsing.element.resolve

import modulecheck.parsing.element.McFile
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.ReferenceName.Companion.asReferenceName

/** A parsing interceptor that unwraps import aliases. */
class ImportAliasUnwrappingParsingInterceptor2 : ParsingInterceptor2 {

  /**
   * Intercepts the parsing process to unwrap import aliases.
   *
   * @param chain The chain of parsing operations.
   * @return The intercepted `ReferenceName`, or `null` if the interception was unsuccessful.
   */
  override suspend fun intercept(chain: ParsingInterceptor2.Chain): ReferenceName? {

    val packet = chain.packet

    // Get the aliased imports from the file, if it's a Kotlin file.
    val aliasedImports = (packet.file as? McFile.McKtFile)
      ?.importAliases
      ?: return chain.proceed(packet)

    // In `Lib1R.string.app_name`, firstSegment is `Lib1R`.
    val firstSegment = packet.toResolve.referenceFirstSegment()

    // If the first segment of the reference name is not an alias, proceed with the chain.
    val alias = aliasedImports[firstSegment] ?: return chain.proceed(packet)

    // Construct a new reference name by replacing the alias with its actual name.
    val newPrefix = alias.name
    val newSuffix = packet.toResolve.segments.drop(1)
      .joinToString(".")

    // Proceed with the chain using the new reference name.
    return chain.proceed(
      packet.copy(
        toResolve = "$newPrefix.$newSuffix".asReferenceName(packet.referenceLanguage)
      )
    )
  }

  /** @return The first segment of the reference name. */
  private fun ReferenceName.referenceFirstSegment(): String = segments.first()
}
