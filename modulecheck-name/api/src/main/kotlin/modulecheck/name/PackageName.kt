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

package modulecheck.name

import modulecheck.name.PackageName.Companion.asPackageName
import modulecheck.name.SimpleName.Companion.asSimpleName

/**
 * Represents a package name.
 *
 * Note that a java/kotlin file without a package declaration will have a `null` _declaration_, but
 * it still has a "default" package. Files with a default package should use [PackageName.DEFAULT].
 *
 * @property asString the full name of this package
 * @see Name
 * @throws IllegalArgumentException if the [asString] parameter is empty or blank
 */
@JvmInline
value class PackageName private constructor(
  override val asString: String
) : Name {

  /**
   * Safe function for appending a simple name to the "end" of a package name.
   *
   * If the package name is default/empty, this function will
   * return just the simple name without a preceding period.
   *
   * If the package name is not blank, this function will append
   * a period to the package name, then add the simple name.
   */
  fun appendAsString(simpleNames: Iterable<SimpleName>): String {
    return "$asString.${simpleNames.joinToString(".") { it.asString }}"
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
  fun append(simpleNames: Iterable<SimpleName>): PackageName {
    return appendAsString(simpleNames).asPackageName()
  }

  override val segments: List<String>
    get() = asString.split('.')

  companion object {

    /**
     * Represents a [PackageName] when there isn't actually a package name, meaning that
     * top-level declarations in that file are at the root of source without qualifiers.
     *
     * @see Name
     * @see DEFAULT
     */
    val DEFAULT = PackageName("")

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
        else -> PackageName(nameOrNull)
      }
    }
  }
}

/** Convenience interface for providing a [PackageName]. */
interface HasPackageName {
  val packageName: PackageName
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
fun PackageName.appendAsString(simpleNames: Iterable<String>): String {
  return appendAsString(simpleNames.map { it.asSimpleName() })
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
fun PackageName.append(simpleNames: Iterable<String>): PackageName {
  return appendAsString(simpleNames).asPackageName()
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
fun PackageName.appendAsString(vararg simpleNames: String): String {
  return appendAsString(simpleNames.toList())
}
