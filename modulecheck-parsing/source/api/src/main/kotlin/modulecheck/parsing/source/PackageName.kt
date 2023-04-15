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

package modulecheck.parsing.source

import kotlinx.serialization.Serializable
import modulecheck.parsing.source.PackageName.DEFAULT
import modulecheck.utils.lazy.unsafeLazy

/**
 * Represents a package name.
 *
 * Note that a java/kotlin file without a package declaration will have a `null` _declaration_, but
 * it still has a "default" package. Files with a default package should use [PackageName.DEFAULT].
 *
 * @see McName
 * @see DEFAULT
 */
@Serializable
sealed interface PackageName : McName {
  /** the full name of this package */
  override val name: String

  /**
   * Represents a [PackageName] when there isn't actually a package name, meaning that
   * top-level declarations in that file are at the root of source without qualifiers.
   *
   * @see McName
   * @see DEFAULT
   */
  @Serializable
  object DEFAULT : PackageName {
    override val name: String = ""

    override fun append(simpleNames: Iterable<String>): String = simpleNames.joinToString(".")
    override val segments: List<String>
      get() = emptyList()
  }

  /**
   * Safe function for appending a simple name to the "end" of a package name.
   *
   * If the package name is default/empty, this function will
   * return just the simple name without a preceding period.
   *
   * If the package name is not blank, this function will append
   * a period to the package name, then add the simple name.
   */
  fun append(simpleNames: Iterable<String>): String

  companion object {
    /** Shorthand for calling [PackageName.invoke] in-line. */
    fun String?.asPackageName(): PackageName = PackageName(this)

    /**
     * Shorthand for calling [PackageName.invoke] in-line.
     *
     * @return A `PackageName` wrapper around [nameOrNull]. If [nameOrNull]
     *   is null or blank, this will return [PackageName.DEFAULT].
     */
    operator fun invoke(nameOrNull: String?): PackageName {
      return when {
        nameOrNull.isNullOrBlank() -> DEFAULT
        else -> PackageNameImpl(nameOrNull)
      }
    }
  }
}

/**
 * Safe function for appending a simple name to the "end" of a package name.
 *
 * If the package name is default/empty, this function will
 * return just the simple name without a preceding period.
 *
 * If the package name is not blank, this function will append
 * a period to the package name, then add the simple name.
 */
fun PackageName.append(vararg simpleNames: String): String = append(simpleNames.toList())

/**
 * @property name the full name of this package
 * @see McName
 * @throws IllegalArgumentException if the [name] parameter is empty or blank
 */
@Serializable
data class PackageNameImpl internal constructor(override val name: String) : PackageName {
  init {
    require(name.isNotBlank()) {
      "A ${this.javaClass.canonicalName} must be a non-empty, non-blank String.  " +
        "Represent an empty/blank or missing package name as ${DEFAULT::class.qualifiedName}.  " +
        "This name argument, wrapped in single quotes: '$name'"
    }
  }

  override fun append(simpleNames: Iterable<String>): String =
    "$name.${simpleNames.joinToString(".")}"

  override val segments: List<String> by unsafeLazy { name.split('.') }
}

/** Convenience interface for providing a [PackageName]. */
interface HasPackageName {
  val packageName: PackageName
}
