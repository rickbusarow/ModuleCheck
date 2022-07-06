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

import modulecheck.parsing.source.McName
import modulecheck.parsing.source.PackageName
import modulecheck.parsing.source.QualifiedDeclaredName
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.internal.NameParser.NameParserPacket

fun interface NameParser {
  suspend fun parse(packet: NameParserPacket): NameParserPacket

  /**
   * @property packageName the package name declared for this associated file
   * @property imports all fully qualified names from import statements
   * @property wildcardImports all wildcard "ullUnder" imports for this file
   * @property aliasedImports a map of alias name to the normal fully qualified name being aliased
   * @property resolved all fully qualified names which have already been resolved
   * @property unresolved all names which have not yet been resolved
   * @property mustBeApi every declaration which is part of the public api
   * @property apiReferenceNames every reference which is part of the public api
   * @property referenceLanguage is this file Java or Kotlin?
   * @property stdLibNameOrNull returns a [ReferenceName] if the receiver name is part of the stdlib
   *   of this [referenceLanguage], otherwise null
   */
  data class NameParserPacket(
    val packageName: PackageName,
    val imports: Set<String>,
    val wildcardImports: Set<String>,
    val aliasedImports: Map<String, ReferenceName>,
    val resolved: Set<ReferenceName>,
    val unresolved: Set<String>,
    // should be a subset of [unresolved]
    val mustBeApi: Set<String>,
    val apiReferenceNames: Set<ReferenceName>,
    val referenceLanguage: McName.CompatibleLanguage,
    val stdLibNameOrNull: String.() -> QualifiedDeclaredName?
  ) {
    override fun toString(): String {
      return """NameParserPacket(
        |packageName='${packageName.name}',
        |
        |imports=$imports,
        |
        |wildcardImports=$wildcardImports,
        |
        |aliasedImports=$aliasedImports,
        |
        |resolved=$resolved,
        |
        |unresolved=$unresolved,
        |
        |mustBeApi=$mustBeApi,
        |
        |apiReferences=$apiReferenceNames
        |
        |)
      """.trimMargin()
    }
  }
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
