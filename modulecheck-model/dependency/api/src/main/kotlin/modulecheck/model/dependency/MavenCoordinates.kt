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

package modulecheck.model.dependency

import modulecheck.utils.lazy.unsafeLazy
import modulecheck.utils.segments
import java.io.File

data class MavenCoordinates(
  /**
   * In `com.google.dagger:dagger:2.32`, this is `com.google.dagger:__:__`.
   *
   * @since 0.12.0
   */
  val group: String?,

  /**
   * In `com.google.dagger:dagger:2.32`, this is `__:dagger:__`.
   *
   * @since 0.12.0
   */
  val moduleName: String,

  /**
   * In `com.google.dagger:dagger:2.32`, this is `__:__:2.32`.
   *
   * @since 0.12.0
   */
  val version: String?
) : Identifier {

  override val name: String by unsafeLazy { "${group.orEmpty()}:$moduleName:${version.orEmpty()}" }

  companion object {

    private val MATCHER = """([\w\.]+):([\w\-]+):([\w\.]+)""".toRegex()

    fun parseOrNull(coordinateString: String): MavenCoordinates? {
      return MATCHER.find(coordinateString)
        ?.destructured
        ?.let { (group, moduleName, version) ->
          MavenCoordinates(group, moduleName, version)
        }
    }

    /**
     * Given a gradle cache path like:
     * ```
     * [...]/com.square.anvil/compiler/1.0.0/911d07691411f7cbccf00d177ac41c1af38/compiler-1.0.0.jar
     * ```
     *
     * Parse out the group, module, and version.
     *
     * @since 0.13.0
     */
    fun File.parseMavenCoordinatesFromGradleCache(): MavenCoordinates? {
      // after `segments()`, we get:
      // [..., "com.square.anvil", "compiler", "1.0.0", "91...38", "compiler-1.0.0.jar"]
      @Suppress("MagicNumber")
      return segments()
        .dropLast(2) // becomes [..., "com.square.anvil", "compiler", "1.0.0"]
        .takeLast(3) // becomes ["com.square.anvil", "compiler", "1.0.0"]
        .takeIf { it.size == 3 }
        ?.joinToString(":") // becomes "com.square.anvil:compiler:1.0.0"
        ?.let { parseOrNull(it) }
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as MavenCoordinates

    if (group != other.group) return false
    if (moduleName != other.moduleName) return false
    // if either version is null (or both), that's a wildcard, and they match
    if (version != null && other.version != null && version != other.version) return false

    return true
  }

  override fun hashCode(): Int {
    var result = group?.hashCode() ?: 0
    result = 31 * result + moduleName.hashCode()
    result = 31 * result + (version?.hashCode() ?: 0)
    return result
  }
}

sealed interface Identifier : Comparable<Identifier> {
  val name: String

  override fun compareTo(other: Identifier): Int {
    return name.compareTo(other.name)
  }
}

sealed class AndroidSdk : Identifier {
  abstract val version: Int

  data class Full(override val version: Int) : AndroidSdk() {
    override val name: String = "android-sdk-jar-$version-full"
  }

  data class CoreForSystemModules(override val version: Int) : AndroidSdk() {
    override val name: String = "android-sdk-jar-$version-core"
  }

  override fun toString(): String = "AndroidSdk($name)"

  companion object {

    private val MATCHER = """.*\/sdk\/platforms\/android-(\d{2})\/([^\/\.]*)\.jar""".toRegex()

    /**
     * Given a gradle cache path like:
     * ```
     * /Users/rbusarow/Library/Android/sdk/platforms/android-30/android.jar
     * ```
     *
     * Parse out the group, module, and version.
     *
     * @since 0.13.0
     */
    fun File.parseAndroidSdkJarFromPath(): AndroidSdk? {
      return MATCHER.find(absolutePath)
        ?.destructured
        ?.let { (version, type) ->
          when (type) {
            "android" -> Full(version.toInt())
            "core-for-system-modules" -> CoreForSystemModules(version.toInt())
            else -> error("unrecognized android sdk artifact type: $type")
          }
        }
    }
  }
}
