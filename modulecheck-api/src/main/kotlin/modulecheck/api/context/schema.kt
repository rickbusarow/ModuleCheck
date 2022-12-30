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

@file:UseSerializers(FileAsStringSerializer::class)

package modulecheck.api.context

import jdk.internal.org.jline.utils.Colors
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.protobuf.ProtoBuf
import modulecheck.api.context.Color_Proto.Blue
import modulecheck.api.context.Color_Proto.Red
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

@OptIn(ExperimentalSerializationApi::class)
fun main() {
  val descriptors = listOf(
    Colors.serializer().descriptor
  )

  val protoBuf = ProtoBuf { encodeDefaults = true }

  val colors = Colors(listOf(Blue, Red, Blue))

  val encoded = protoBuf.encodeToByteArray(colors)

  println(protoBuf.encodeToHexString(colors))

  val decoded = ProtoBuf.decodeFromByteArray<Colors>(encoded)

  check(colors == decoded)
  println(colors == decoded)
  println(decoded)
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
