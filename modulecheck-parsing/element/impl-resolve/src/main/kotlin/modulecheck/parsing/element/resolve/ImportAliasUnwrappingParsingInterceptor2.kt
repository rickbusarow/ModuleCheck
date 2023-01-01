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

import modulecheck.parsing.element.McFile
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.ReferenceName.Companion.asReferenceName

class ImportAliasUnwrappingParsingInterceptor2 : ParsingInterceptor2 {

  override suspend fun intercept(
    chain: ParsingInterceptor2.Chain
  ): ReferenceName? {

    val packet = chain.packet

    val aliasedImports = (packet.file as? McFile.McKtFile)
      ?.importAliases
      ?: return chain.proceed(packet)

    // in `Lib1R.string.app_name`, firstSegment is `Lib1R`
    val firstSegment = packet.toResolve.referenceFirstSegment()

    val alias = aliasedImports[firstSegment] ?: return chain.proceed(packet)

    val newPrefix = alias.name
    val newSuffix = packet.toResolve.segments.drop(1)
      .joinToString(".")

    return chain.proceed(
      packet.copy(
        toResolve = "$newPrefix.$newSuffix".asReferenceName(packet.referenceLanguage)
      )
    )
  }

  private fun ReferenceName.referenceFirstSegment(): String = segments.first()
}
