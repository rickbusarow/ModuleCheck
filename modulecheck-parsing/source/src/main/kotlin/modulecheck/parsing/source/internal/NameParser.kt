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
import modulecheck.parsing.source.Reference.ExplicitReference
import modulecheck.parsing.source.internal.NameParser.NameParserPacket

fun interface NameParser {
  suspend fun parse(packet: NameParserPacket): NameParserPacket

  data class NameParserPacket(
    val packageName: String,
    val imports: Set<String>,
    val wildcardImports: Set<String>,
    val aliasedImports: Map<String, ExplicitReference>,
    val resolved: Set<Reference>,
    val unresolved: Set<String>,
    // should be a subset of [unresolved]
    val mustBeApi: Set<String>,
    val apiReferences: Set<Reference>,
    val toExplicitReference: String.() -> Reference.ExplicitReference,
    val toInterpretedReference: String.() -> Reference.InterpretedReference,
    val stdLibNameOrNull: String.() -> ExplicitReference?
  )
}

class ParsingChain private constructor(
  override val packet: NameParserPacket,
  private val interceptors: List<ParsingInterceptor>
) : ParsingInterceptor.Chain {

  override suspend fun proceed(packet: NameParserPacket): NameParserPacket {
    val next = ParsingChain(packet, interceptors.drop(1))

    val interceptor = interceptors.first()

    return interceptor.intercept(next)
  }

  class Factory(
    private val interceptors: List<ParsingInterceptor>
  ) : NameParser {

    override suspend fun parse(packet: NameParserPacket): NameParserPacket {

      return ParsingChain(packet, interceptors).proceed(packet)
    }
  }
}

fun interface ParsingInterceptor {

  suspend fun intercept(chain: Chain): NameParserPacket

  interface Chain {
    val packet: NameParserPacket

    /** Passes the [packet] argument on to the next interceptor in this chain. */
    suspend fun proceed(packet: NameParserPacket): NameParserPacket
  }
}
