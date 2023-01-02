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

@file:UseSerializers(FileAsStringSerializer::class)

package modulecheck.parsing.element

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.schema.ProtoBufSchemaGenerator
import modulecheck.parsing.element.McVisibility.McKtVisibility
import modulecheck.parsing.element.Top.MiddleA
import modulecheck.parsing.source.DeclaredName
import modulecheck.parsing.source.PackageName
import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.SimpleName
import modulecheck.utils.serialization.FileAsStringSerializer
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction

// TODO <Rick> delete me
@OptIn(ExperimentalSerializationApi::class)
fun main() {

  val descriptors = listOf(
    RealMcKtFunction2.serializer().descriptor
    // RealMcKtFile2.serializer().descriptor
    // ReferenceName.serializer().descriptor
  )

  ProtoBufSchemaGenerator.generateSchemaText(descriptors)
    .also(::println)

  val protoBuf = ProtoBuf { encodeDefaults = true }
}


@Serializable
data class Inside(
  val thing: Top
) : MiddleA

@Serializable
sealed interface Top {
  @Serializable
  sealed interface MiddleA : Top

  @Serializable
  sealed interface MiddleB : Top
}

@Serializable
data class RealMcKtFunction2(
  val parent: McKtElement,
  val parameters: Set<RealMcKtParameter2>,
  val properties: Set<RealMcKtProperty2>,
  val returnType: ReferenceName,
  val typeParamters: Set<RealMcKtTypeParameter2>,
  val annotations: Set<RealMcKtAnnotation2>,
  val typeParameters: Set<RealMcKtTypeParameter2>,
  val visibility: McKtVisibility,
  @Transient
  val psi: KtFunction? = null
)

@Serializable
data class RealMcKtParameter2(
  val index: Int,
  val parent: McKtElement,
  val psi: KtCallableDeclaration?,
  val annotations: Set<RealMcKtAnnotation2>,
  val visibility: McKtVisibility
)

@Serializable
data class RealMcKtAnnotation2(
  val psi: KtElement?,
  val parent: McKtElement,
  val referenceName: ReferenceName?
)

@Serializable
data class RealMcKtTypeParameter2(
  val psi: KtElement?,
  val parent: McKtElement,
  val annotations: Set<RealMcKtAnnotation2>,
  val typeParameters: Set<RealMcKtTypeParameter2>
  // val superTypes: Set<McType>
)

@Serializable
data class RealMcKtProperty2(
  val index: Int,
  val parent: McKtElement,
  @Transient
  val psi: KtCallableDeclaration? = null,
  val annotations: Set<RealMcKtTypeParameter2>,
  val visibility: McKtVisibility,
  val typeReferenceName: ReferenceName,
  val isMutable: Boolean,
  val declaredName: DeclaredName,
  val packageName: PackageName,
  val simpleNames: List<SimpleName>
)

// @Serializable
// data class RealMcKtFile2(
//   override val annotations: LazySet<McAnnotation>,
//   override val file: File,
//   override val imports: DataSource<ReferenceName>,
//   override val apiReferences: List<DataSource<ReferenceName>>,
//   override val references: List<DataSource<ReferenceName>>,
//   override val declarations: List<DataSource<DeclaredName>>,
//   override val declaredTypes: LazySet<McKtConcreteType>,
//   override val declaredTypesAndInnerTypes: LazySet<McKtConcreteType>,
//   override val topLevelFunctions: LazySet<McFunction>,
//   override val topLevelProperties: LazySet<McProperty>,
//   override val importAliases: Map<String, ReferenceName>,
//   override val wildcardImports: DataSource<String>,
//   override val packageName: PackageName,
//   @Transient
//   override val psi: KtFile? = null
// ) : McKtFile {
//   override suspend fun getAnvilScopeArguments(
//     allAnnotations: List<ReferenceName>,
//     mergeAnnotations: List<ReferenceName>
//   ): ScopeArgumentParseResult {
//     TODO("Not yet implemented")
//   }
// }
