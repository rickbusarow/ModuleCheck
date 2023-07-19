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

package modulecheck.model.dependency

import modulecheck.model.dependency.AndroidSdk.CoreForSystemModules
import modulecheck.model.dependency.AndroidSdk.Full
import modulecheck.utils.lazy.unsafeLazy
import modulecheck.utils.segments
import java.io.File

/** ex: `com.google.dagger:dagger:2.32` */
data class MavenCoordinates(
  override val group: String?,
  override val moduleName: String,
  override val version: String?
) : Identifier, HasMavenCoordinatesElements {

  override val name: String by unsafeLazy { "${group.orEmpty()}:$moduleName:${version.orEmpty()}" }

  companion object {

    private val MATCHER = """([\w.]+):([\w\-.]+):([\w.\-]+)""".toRegex()

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
}

/** Some sort of name */
sealed interface Identifier : Comparable<Identifier> {
  val name: String

  override fun compareTo(other: Identifier): Int {
    return name.compareTo(other.name)
  }
}

/**
 * Models the [name][java.io.File.getName] of an Android SDK .jar
 * file. This will be either [Full] or [CoreForSystemModules].
 */
sealed class AndroidSdk : Identifier {
  /** The SDK **number**, like `29` or `32` */
  abstract val version: Int

  /** `android-sdk-jar-$version-full` */
  data class Full(override val version: Int) : AndroidSdk() {
    override val name: String = "android-sdk-jar-$version-full"
  }

  /** `android-sdk-jar-$version-core` */
  data class CoreForSystemModules(override val version: Int) : AndroidSdk() {
    override val name: String = "android-sdk-jar-$version-core"
  }

  override fun toString(): String = "AndroidSdk($name)"

  companion object {

    private val MATCHER = """.*/sdk/platforms/android-(\d{2})/([^/.]*)\.jar""".toRegex()

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
