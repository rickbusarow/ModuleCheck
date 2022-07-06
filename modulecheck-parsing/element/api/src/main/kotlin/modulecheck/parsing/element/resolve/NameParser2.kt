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
import modulecheck.parsing.element.resolve.NameParser2.NameParser2Packet
import modulecheck.parsing.element.resolve.ParsingInterceptor2.Chain
import modulecheck.parsing.source.McName
import modulecheck.parsing.source.ReferenceName

fun interface NameParser2 {
  suspend fun parse(packet: NameParser2Packet): ReferenceName?

  /**
   * @property file the file being parsed
   * @property toResolve the reference name to be resolved
   * @property referenceLanguage is this file Java or Kotlin?
   * @property stdLibNameOrNull returns a [ReferenceName] if the receiver name is part of the stdlib
   *   of this [referenceLanguage], otherwise null
   */
  data class NameParser2Packet(
    val file: McFile,
    val toResolve: ReferenceName,
    val referenceLanguage: McName.CompatibleLanguage,
    val stdLibNameOrNull: ReferenceName.() -> ReferenceName?
  )
}

fun interface ParsingInterceptor2 {

  suspend fun intercept(chain: Chain): ReferenceName?

  interface Chain {
    val packet: NameParser2Packet

    /** Passes the [packet] argument on to the next interceptor in this chain. */
    suspend fun proceed(packet: NameParser2Packet): ReferenceName?
  }
}

class ParsingChain2 private constructor(
  override val packet: NameParser2Packet,
  private val interceptors: List<ParsingInterceptor2>
) : Chain {

  override suspend fun proceed(packet: NameParser2Packet): ReferenceName? {
    val next = ParsingChain2(packet, interceptors.drop(1))

    val interceptor = interceptors.first()

    return interceptor.intercept(next)
  }

  class Factory(
    private val interceptors: List<ParsingInterceptor2>
  ) : NameParser2 {

    override suspend fun parse(packet: NameParser2Packet): ReferenceName? {

      return ParsingChain2(packet, interceptors).proceed(packet)
    }
  }
}
