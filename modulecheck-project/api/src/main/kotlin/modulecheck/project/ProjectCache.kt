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
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import modulecheck.model.dependency.ProjectPath
import modulecheck.model.dependency.ProjectPath.TypeSafeProjectPath
import modulecheck.utils.requireNotNull
import modulecheck.utils.trace.HasTraceTags
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class ProjectCache : HasTraceTags {
  private val delegate = ConcurrentHashMap<TypeSafeProjectPath, McProject>()

  override val tags: List<KClass<out ProjectCache>> = listOf(this::class)

  val values: MutableCollection<McProject> get() = delegate.values

  /**
   * Retrieves a project from the cache or puts a new one if it doesn't exist.
   * All project path pugs and gets are done using the derived type-safe variant.
   *
   * @param path The path of the project.
   * @param defaultValue The function that generates a
   *   default project if it doesn't exist in the cache.
   * @return The existing or newly added project.
   * @since 0.12.0
   */
  fun getOrPut(path: ProjectPath, defaultValue: () -> McProject): McProject {
    return delegate.getOrPut(path.asTypeSafeProjectPath(), defaultValue)
  }

  /**
   * Retrieves a project from the cache.
   *
   * @param path The path of the project.
   * @return The project associated with the given path.
   * @throws NullPointerException if no project exists for the given path.
   */
  fun getValue(path: ProjectPath): McProject {
    return delegate[path.asTypeSafeProjectPath()].requireNotNull {
      "Expected to find a project with a path of '${path.value}`, but no such project exists.\n\n" +
        "The existing paths are: ${delegate.keys.map { it.value }}"
    }
  }

  /**
   * Adds or updates a project in the cache.
   *
   * @param path The path of the project.
   * @param project The project to be added or updated.
   * @return The previous project associated with the
   *   path, or null if there was no mapping for the path.
   */
  operator fun set(path: ProjectPath, project: McProject): McProject? {
    return delegate.put(path.asTypeSafeProjectPath(), project)
  }

  fun clearContexts() {
    delegate.values.forEach { it.clearContext() }
  }
}

/** */
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
