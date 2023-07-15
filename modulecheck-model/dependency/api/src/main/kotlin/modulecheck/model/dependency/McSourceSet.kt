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

package modulecheck.model.dependency

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import modulecheck.model.sourceset.HasSourceSetName
import modulecheck.model.sourceset.SourceSetName
import modulecheck.parsing.kotlin.compiler.KotlinEnvironment
import modulecheck.utils.lazy.LazyDeferred
import modulecheck.utils.sequenceOfNotNull
import modulecheck.utils.serialization.FileAsStringSerializer
import org.jetbrains.kotlin.config.JvmTarget
import java.io.File

interface HasSourceSets {
  val sourceSets: SourceSets
}

/** Cache of [sourceSets][McSourceSet], probably at the project level. */
@Serializable(with = SourceSetsSerializer::class)
class SourceSets(
  delegate: Map<SourceSetName, McSourceSet>
) : Map<SourceSetName, McSourceSet> by delegate

/** */
object SourceSetsSerializer : KSerializer<SourceSets> {

  private val delegate: KSerializer<Map<SourceSetName, McSourceSet>> = serializer()

  override val descriptor = delegate.descriptor

  override fun serialize(encoder: Encoder, value: SourceSets) {
    encoder.encodeSerializableValue(delegate, value)
  }

  override fun deserialize(decoder: Decoder): SourceSets {
    return SourceSets(decoder.decodeSerializableValue(delegate))
  }
}

/**
 * Models all the particulars for a compilation unit, roughly equivalent
 * to the source set models in AGP, KGP, and the Java Gradle Plugin.
 *
 * @property name the name of this source set, like 'main' or 'internalRelease'
 * @property compileOnlyConfiguration the configuration name of this source set's 'compileOnly'
 *   configuration, like 'compileOnly' for 'main' or 'debugCompileOnly' for 'debug'
 * @property apiConfiguration the configuration name of this source set's
 *   'api' configuration, like 'api' for 'main' or 'debugApi' for 'debug'
 * @property implementationConfiguration the configuration name
 *   of this source set's 'implementation' configuration, like
 *   'implementation' for 'main' or 'debugImplementation' for 'debug'
 * @property runtimeOnlyConfiguration the configuration name of this source set's 'runtimeOnly'
 *   configuration, like 'runtimeOnly' for 'main' or 'debugRuntimeOnly' for 'debug'
 * @property annotationProcessorConfiguration the configuration name
 *   of this source set's 'annotationProcessor' configuration, like
 *   'annotationProcessor' for 'main' or 'debugAnnotationProcessor' for 'debug'
 * @property jvmFiles all java/kotlin/scala/groovy files in this source set
 * @property resourceFiles all xml 'res' files for this source set
 * @property layoutFiles all android layout files for
 *   this source set. This is a subset of [resourceFiles].
 * @property jvmTarget the Java version used when compiling this source set
 * @property kotlinEnvironmentDeferred the kotlin environment
 *   used for "compiling" and parsing this source set
 * @property upstreamLazy all source sets upstream of
 *   this one, like `main` if this source set is `test`
 * @property downstreamLazy all source sets downstream of
 *   this one, like `test` if this source set is `main`
 * @since 0.12.0
 */
@Serializable
class McSourceSet(
  val name: SourceSetName,
  val compileOnlyConfiguration: McConfiguration,
  val apiConfiguration: McConfiguration?,
  val implementationConfiguration: McConfiguration,
  val runtimeOnlyConfiguration: McConfiguration,
  val annotationProcessorConfiguration: McConfiguration?,
  val jvmFiles: Set<File>,
  val resourceFiles: Set<File>,
  val layoutFiles: Set<File>,
  val jvmTarget: JvmTarget,
  val kotlinEnvironmentDeferred: LazyDeferred<KotlinEnvironment>,
  private val upstreamLazy: Lazy<List<SourceSetName>>,
  private val downstreamLazy: Lazy<List<SourceSetName>>
) : Comparable<McSourceSet>, HasSourceSetName {

  override val sourceSetName: SourceSetName get() = name

  val configurations: Configurations by lazy {
    Configurations(
      sequenceOfNotNull(
        compileOnlyConfiguration,
        apiConfiguration,
        implementationConfiguration,
        runtimeOnlyConfiguration,
        annotationProcessorConfiguration
      ).associateBy { it.name }
    )
  }

  val projectDependencies: ProjectDependencies by lazy {
    ProjectDependencies(
      configurations.mapValues { (_, configuration) ->
        configuration.projectDependencies
      }
    )
  }

  val externalDependencies: ExternalDependencies by lazy {
    ExternalDependencies(
      configurations.mapValues { (_, configuration) ->
        configuration.externalDependencies
      }
    )
  }

  /**
   * upstream source set names
   *
   * @since 0.12.0
   */
  val upstream: List<SourceSetName> by upstreamLazy

  /**
   * downstream source set names
   *
   * @since 0.12.0
   */
  val downstream: List<SourceSetName> by downstreamLazy

  fun withUpstream(): List<SourceSetName> = listOf(name) + upstream
  fun withDownstream(): List<SourceSetName> = listOf(name) + downstream

  val hasExistingSourceFiles: Boolean by lazy {
    jvmFiles.hasExistingFiles() ||
      resourceFiles.hasExistingFiles() ||
      layoutFiles.hasExistingFiles()
  }

  private fun Set<File>.hasExistingFiles(): Boolean {
    return any { dir ->
      dir.walkBottomUp()
        .any { file -> file.isFile && file.exists() }
    }
  }

  /**
   * If one source set extends another, then the extended one should come before it in a collection.
   * For instance, in [withUpstream] for `TestDebug`, the list would be `[main, debug, testDebug]`.
   *
   * If two source sets are siblings (neither extends the other, such as in build
   * flavors from different dimensions), then they should be sorted alphabetically
   * (by name). The alphabetical sort just ensures that all lists are stable.
   *
   * @since 0.12.0
   */
  override fun compareTo(other: McSourceSet): Int {

    if (this == other) return 0

    return when {
      upstream.contains(other.name) -> 1
      other.upstream.contains(name) -> -1
      else -> name.value.compareTo(other.name.value)
    }
  }

  override fun toString(): String {
    return """SourceSet(
        |  name=$name,
        |  compileOnlyConfiguration=$compileOnlyConfiguration,
        |  apiConfiguration=$apiConfiguration,
        |  implementationConfiguration=$implementationConfiguration,
        |  runtimeOnlyConfiguration=$runtimeOnlyConfiguration,
        |  kaptConfiguration=$annotationProcessorConfiguration,
        |  jvmFiles=$jvmFiles,
        |  resourceFiles=$resourceFiles,
        |  layoutFiles=$layoutFiles,
        |  upstreamLazy=${upstreamLazy.value.map { it.value }},
        |  downstreamLazy=${downstreamLazy.value.map { it.value }}
        |)
    """.trimMargin()
  }
}

fun Iterable<McSourceSet>.names(): List<SourceSetName> = map { it.name }
fun Sequence<McSourceSet>.names(): Sequence<SourceSetName> = map { it.name }

fun Collection<McSourceSet>.sortedByInheritance(): Sequence<McSourceSet> {

  val pending = sortedBy { it.upstream.size }.toMutableList()
  val history = mutableSetOf<SourceSetName>()

  return generateSequence {

    pending.firstOrNull { pendingSourceSet ->
      // find the first SourceSet where everything upstream has already been yielded to the sequence
      pendingSourceSet.upstream.isEmpty() || pendingSourceSet.upstream.all { history.contains(it) }
    }
      ?.also {
        history.add(it.name)
        pending.remove(it)
      }
  }
}

/** Upstream source set names **not** including the receiver name. */
fun SourceSetName.upstream(hasSourceSets: HasSourceSets): List<SourceSetName> =
  hasSourceSets.sourceSets[this]
    ?.upstream
    .orEmpty()

/**
 * Upstream source set names *including the receiver name.*
 *
 * ### Ordering
 *
 * Order is based upon proximity to the receiver name. This is a breadth-first traversal
 * of a directed graph where the receiver [SourceSetName] is the root. The first
 * returned name is the receiver, followed by the source sets it directly inherits.
 */
fun SourceSetName.withUpstream(hasSourceSets: HasSourceSets): List<SourceSetName> =
  hasSourceSets.sourceSets[this]
    ?.withUpstream()
    .orEmpty()

fun SourceSetName.withDownStream(hasSourceSets: HasSourceSets): List<SourceSetName> =
  hasSourceSets.sourceSets[this]
    ?.withDownstream()
    .orEmpty()

fun SourceSetName.inheritsFrom(other: SourceSetName, hasSourceSets: HasSourceSets): Boolean {

  // SourceSets can't inherit from themselves, so quit early and skip some lookups.
  if (this == other) return false

  return hasSourceSets.sourceSets[this]
    ?.upstream
    ?.contains(other)
    ?: false
}
