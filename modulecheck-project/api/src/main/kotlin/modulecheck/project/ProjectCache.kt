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

package modulecheck.project

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import modulecheck.dagger.SingleIn
import modulecheck.dagger.TaskScope
import modulecheck.model.dependency.PlatformPlugin
import modulecheck.model.dependency.ProjectPath
import modulecheck.model.dependency.ProjectPath.StringProjectPath
import modulecheck.model.dependency.ProjectPath.TypeSafeProjectPath
import modulecheck.parsing.source.AnvilGradlePlugin
import modulecheck.project.JvmFileProvider.Factory
import modulecheck.reporting.logging.McLogger
import modulecheck.utils.requireNotNull
import modulecheck.utils.trace.HasTraceTags
import org.jetbrains.kotlin.config.JvmTarget
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.reflect.KClass

@Serializable
@SingleIn(TaskScope::class)
class ProjectCache @Inject constructor() : HasTraceTags {
  @Serializable(with = ConcurrentHashMapSerializer::class)
  private val delegate = ConcurrentHashMap<TypeSafeProjectPath, McProject>()

  override val tags: List<KClass<out ProjectCache>> = listOf(this::class)

  val values: MutableCollection<McProject> get() = delegate.values

  /**
   * N.B. This [path] argument can be the base [ProjectPath] instead of one of the concrete
   * types ([StringProjectPath], [TypeSafeProjectPath]), because all project paths are compared
   * using the derived type-safe variant. So, there are no cache misses when a project is already
   * stored using the String variant, but then we attempt to look it up via the type-safe one.
   *
   * @since 0.12.0
   */
  fun getOrPut(path: ProjectPath, defaultValue: () -> McProject): McProject {
    return delegate.getOrPut(path.toTypeSafe(), defaultValue)
  }

  fun getValue(path: ProjectPath): McProject {
    return delegate[path].requireNotNull {
      "Expected to find a project with a path of '${path.value}`, but no such project exists.\n\n" +
        "The existing paths are: ${delegate.keys.map { it.value }}"
    }
  }

  operator fun set(path: ProjectPath, project: McProject): McProject? {
    return delegate.put(path.toTypeSafe(), project)
  }

  fun clearContexts() {
    delegate.values.forEach { it.clearContext() }
  }
}

@Serializable
@Suppress("LongParameterList")
class RealMcProject2(
  val projectPath: StringProjectPath,
  val hasKapt: Boolean,
  val hasTestFixturesPlugin: Boolean,
  val anvilGradlePlugin: AnvilGradlePlugin?,
  val logger: McLogger,
  val jvmFileProviderFactory: Factory,
  val jvmTarget: JvmTarget,
  val platformPlugin: PlatformPlugin
)

@Serializable
@SingleIn(TaskScope::class)
class ProjectCache2 @Inject constructor() : HasTraceTags {
  @Serializable(with = ConcurrentHashMapSerializer::class)
  private val delegate = ConcurrentHashMap<TypeSafeProjectPath, RealMcProject2>()

  override val tags: List<KClass<out ProjectCache2>> = listOf(this::class)

  val values: MutableCollection<RealMcProject2> get() = delegate.values

  /**
   * N.B. This [path] argument can be the base [ProjectPath] instead of one of the concrete
   * types ([StringProjectPath], [TypeSafeProjectPath]), because all project paths are compared
   * using the derived type-safe variant. So, there are no cache misses when a project is already
   * stored using the String variant, but then we attempt to look it up via the type-safe one.
   *
   * @since 0.12.0
   */
  fun getOrPut(path: ProjectPath, defaultValue: () -> RealMcProject2): RealMcProject2 {
    return delegate.getOrPut(path.toTypeSafe(), defaultValue)
  }

  fun getValue(path: ProjectPath): RealMcProject2 {
    return delegate[path].requireNotNull {
      "Expected to find a project with a path of '${path.value}`, but no such project exists.\n\n" +
        "The existing paths are: ${delegate.keys.map { it.value }}"
    }
  }

  operator fun set(path: ProjectPath, project: RealMcProject2): RealMcProject2? {
    return delegate.put(path.toTypeSafe(), project)
  }
}

// @Serializable
// data class ProjectCacheSerializable()

internal object ConcurrentHashMapSerializer :
  KSerializer<ConcurrentHashMap<ProjectPath, McProject>> {

  private val delegate: KSerializer<Map<ProjectPath, McProject>> = serializer()

  override val descriptor = delegate.descriptor

  override fun serialize(encoder: Encoder, value: ConcurrentHashMap<ProjectPath, McProject>) {
    encoder.encodeSerializableValue(delegate, value)
  }

  override fun deserialize(decoder: Decoder): ConcurrentHashMap<ProjectPath, McProject> {
    return ConcurrentHashMap(decoder.decodeSerializableValue(delegate))
  }
}
