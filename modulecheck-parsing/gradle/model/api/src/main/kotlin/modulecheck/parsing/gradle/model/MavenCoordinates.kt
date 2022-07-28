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

package modulecheck.parsing.gradle.model

import modulecheck.utils.lazy.unsafeLazy

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
) : Identifier, Comparable<MavenCoordinates> {

  override val name: String by unsafeLazy { "${group ?: ""}:$moduleName:${version ?: ""}" }

  companion object {

    private val MATCHER = "([\\w\\.]+):([\\w\\-]+):([\\w\\.]+)".toRegex()

    fun parseOrNull(coordinateString: String): MavenCoordinates? {
      return MATCHER.find(coordinateString)
        ?.destructured
        ?.let { (group, moduleName, version) ->
          MavenCoordinates(group, moduleName, version)
        }
    }
  }

  override fun compareTo(other: MavenCoordinates): Int {
    return name.compareTo(other.name)
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

sealed interface Identifier {
  val name: String
}
