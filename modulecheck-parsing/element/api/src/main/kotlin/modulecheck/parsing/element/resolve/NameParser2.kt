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
import modulecheck.parsing.element.resolve.NameParser2.NameParser2Packet
import modulecheck.parsing.source.McName
import modulecheck.parsing.source.QualifiedDeclaredName
import modulecheck.parsing.source.ReferenceName

/**
 * Intercepts parsing operations. Implementations of this interface should
 * provide a way to parse a given `NameParser2Packet` into a `ReferenceName`.
 */
fun interface NameParser2 {
  /**
   * Parses the given packet into a `ReferenceName`.
   *
   * @param packet The packet to parse.
   * @return The parsed `ReferenceName`, or `null` if parsing was unsuccessful.
   */
  suspend fun parse(packet: NameParser2Packet): ReferenceName?

  /**
   * @property file The file being parsed.
   * @property toResolve The reference name to be resolved.
   * @property referenceLanguage The language of the file (Java or Kotlin).
   * @property stdLibNameOrNull A function that returns a `QualifiedDeclaredName` if the
   *   receiver name is part of the stdlib of this `referenceLanguage`, otherwise null.
   */
  data class NameParser2Packet(
    val file: McFile,
    val toResolve: ReferenceName,
    val referenceLanguage: McName.CompatibleLanguage,
    val stdLibNameOrNull: ReferenceName.() -> QualifiedDeclaredName?
  )
}

/**
 * Intercepts parsing operations. Implementations of this interface should provide
 * a way to intercept the parsing process and potentially modify the result.
 */
fun interface ParsingInterceptor2 {

  /**
   * Intercepts the parsing process.
   *
   * @param chain The chain of parsing operations.
   * @return The intercepted `ReferenceName`, or `null` if the interception was unsuccessful.
   */
  suspend fun intercept(chain: Chain): ReferenceName?

  /** Represents a chain of parsing operations. */
  interface Chain {
    /** */
    val packet: NameParser2Packet

    /**
     * Passes the `packet` argument on to the next interceptor in this chain.
     *
     * @param packet The packet to pass on.
     * @return The result of the next interceptor in the
     *   chain, or `null` if there are no more interceptors.
     */
    suspend fun proceed(packet: NameParser2Packet): ReferenceName?
  }
}

/**
 * Represents a chain of parsing operations.
 *
 * @property packet The packet to be parsed.
 * @property interceptors The list of interceptors in the chain.
 */
class ParsingChain2 private constructor(
  override val packet: NameParser2Packet,
  private val interceptors: List<ParsingInterceptor2>
) : ParsingInterceptor2.Chain {

  /**
   * Passes the `packet` argument on to the next interceptor in this chain.
   *
   * @param packet The packet to pass on.
   * @return The result of the next interceptor in the
   *   chain, or `null` if there are no more interceptors.
   */
  override suspend fun proceed(packet: NameParser2Packet): ReferenceName? {
    val next = ParsingChain2(packet, interceptors.drop(1))

    val interceptor = interceptors.first()

    return interceptor.intercept(next)
  }

  /**
   * Factory for creating instances of `ParsingChain2`.
   *
   * @property interceptors The list of interceptors to include in the chain.
   */
  class Factory(
    private val interceptors: List<ParsingInterceptor2>
  ) : NameParser2 {

    /**
     * Parses the given packet into a `ReferenceName` using the chain of interceptors.
     *
     * @param packet The packet to parse.
     * @return The parsed `ReferenceName`, or `null` if parsing was unsuccessful.
     */
    override suspend fun parse(packet: NameParser2Packet): ReferenceName? {

      return ParsingChain2(packet, interceptors).proceed(packet)
    }
  }
}
