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

package modulecheck.parsing.source

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.swiftzer.semver.SemVer
import org.jetbrains.kotlin.name.FqName

@Serializable
data class AnvilGradlePlugin(
  @Serializable(SemVerSerializer::class)
  val version: SemVer,
  val generateDaggerFactories: Boolean
)

data class AnvilAnnotatedType(
  val contributedTypeDeclaration: QualifiedDeclaredName,
  val contributedScope: AnvilScopeName
)

data class RawAnvilAnnotatedType(
  val declaredName: QualifiedDeclaredName,
  val anvilScopeNameEntry: AnvilScopeNameEntry
)

data class AnvilScopeName(val fqName: FqName) {
  override fun toString(): String = fqName.asString()
}

data class AnvilScopeNameEntry(val name: ReferenceName)

@Serializable
internal data class SemVerSurrogate(
  val major: Int = 0,
  val minor: Int = 0,
  val patch: Int = 0,
  val preRelease: String? = null,
  val buildMetadata: String? = null
)

object SemVerSerializer : KSerializer<SemVer> {
  override val descriptor: SerialDescriptor = SemVerSurrogate.serializer().descriptor

  override fun serialize(encoder: Encoder, value: SemVer) {
    val surrogate = SemVerSurrogate(
      value.major,
      value.minor,
      value.patch,
      value.preRelease,
      value.buildMetadata
    )
    encoder.encodeSerializableValue(SemVerSurrogate.serializer(), surrogate)
  }

  override fun deserialize(decoder: Decoder): SemVer {
    val surrogate = decoder.decodeSerializableValue(SemVerSurrogate.serializer())
    return SemVer(
      surrogate.major,
      surrogate.minor,
      surrogate.patch,
      surrogate.preRelease,
      surrogate.buildMetadata
    )
  }
}
