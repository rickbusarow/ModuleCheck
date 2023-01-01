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

package modulecheck.api.context

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.schema.ProtoBufSchemaGenerator
import modulecheck.model.dependency.ConfigurationName
import modulecheck.model.dependency.ConfiguredDependency
import modulecheck.model.dependency.ExternalDependency
import modulecheck.model.dependency.McConfiguration
import modulecheck.model.dependency.ProjectDependency
import modulecheck.model.dependency.TransitiveProjectDependency
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.android.XmlFile.LayoutFile
import modulecheck.parsing.android.XmlFile.ManifestFile
import modulecheck.parsing.source.AndroidRDeclaredName
import modulecheck.parsing.source.JvmFile
import modulecheck.parsing.source.ReferenceName
import modulecheck.utils.serialization.FileAsStringSerializer
import org.jetbrains.kotlin.config.JvmTarget
import java.io.File

// TODO <Rick> delete me
@OptIn(ExperimentalSerializationApi::class)
fun main() {

  val descriptors = listOf(
    ReferenceName_Proto.serializer().descriptor
    // ReferenceName.serializer().descriptor
  )

  ProtoBufSchemaGenerator.generateSchemaText(descriptors)
    .also(::println)

  val protoBuf = ProtoBuf { encodeDefaults = true }

  println(
    protoBuf.encodeToHexString(
      ReferenceName_Proto(
        "Butt",
        null,
        listOf(),
        CompatibleLanguage_Proto.KOTLIN
      )
    )
  )
}

@Serializable
data class ReferenceName_Proto(
  val simpleName: String,
  val receiver: String?,
  val typeArguments: List<TypeName_Proto>,
  val language: CompatibleLanguage_Proto
)

@Serializable
data class TypeName_Proto(
  val packageName: String?,
  val enclosingType: Type_Proto?,
  val simpleNames: List<String>,
  val simpleName: String?
)

@Serializable
data class Type_Proto(
  val packageName: String?,
  val enclosingType: Type_Proto?,
  val simpleNames: List<String>,
  val simpleName: String?,
  val nestedTypes: List<Type_Proto>
)

@Serializable
enum class CompatibleLanguage_Proto {
  JAVA,
  KOTLIN,
  XML
}

@Serializable
class McSourceSet_Proto(
  val name: SourceSetName,
  val compileOnlyConfiguration: McConfiguration_Proto,
  val apiConfiguration: McConfiguration_Proto?,
  val implementationConfiguration: McConfiguration_Proto,
  val runtimeOnlyConfiguration: McConfiguration_Proto,
  val annotationProcessorConfiguration: McConfiguration_Proto?,
  val jvmFiles: Set<JvmFile>,
  val resourceFiles: Set<File>,
  val layoutFiles: List<LayoutFile>,
  val jvmTarget: JvmTarget,
  val androidRDeclaredNames: List<AndroidRDeclaredName>,
  val sourceSetDependencies: List<TransitiveProjectDependency>,
  val references: List<ReferenceName>,
  val manifestFile: ManifestFile?,
  val upstream: List<SourceSetName>,
  val downstream: List<SourceSetName>
)

@Serializable
data class McConfiguration_Proto(
  val name: ConfigurationName,
  val projectDependencies: Set<ProjectDependency>,
  val externalDependencies: Set<ExternalDependency>,
  val kaptDependencies: Set<ConfiguredDependency>,
  private val upstreamSequence: Sequence<McConfiguration>,
  private val downstreamSequence: Sequence<McConfiguration>
)
