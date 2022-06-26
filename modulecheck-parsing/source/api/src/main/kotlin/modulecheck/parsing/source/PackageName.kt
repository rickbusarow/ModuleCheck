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

package modulecheck.parsing.source

import modulecheck.parsing.source.PackageName.DEFAULT

/**
 * Represents a package name.
 *
 * Note that a java/kotlin file without a package declaration will have a `null` _declaration_, but
 * it still has a "default" package. Files with a default package should use [PackageName.DEFAULT].
 *
 * @see McName
 * @see DEFAULT
 * @since 0.13.0
 */
sealed interface PackageName : McName {
  /** the full name of this package */
  override val name: String

  /**
   * Represents a [PackageName] when there isn't actually a package name, meaning that top-level
   * declarations in that file are at the root of source without qualifiers.
   *
   * @see McName
   * @see DEFAULT
   * @since 0.13.0
   */
  object DEFAULT : PackageName {
    override val name: String = ""

    override fun append(simpleName: String): String = simpleName
  }

  /**
   * Safe function for appending a simple name to the "end" of a package name.
   *
   * If the package name is default/empty, this function will return just the simple name without a
   * preceding period.
   *
   * If the package name is not blank, this function will append a period to the package name, then
   * add the simple name.
   *
   * @since 0.13.0
   */
  fun append(simpleName: String): String

  companion object {
    operator fun invoke(nameOrNull: String?): PackageName {
      return when {
        nameOrNull.isNullOrBlank() -> DEFAULT
        else -> PackageNameImpl(nameOrNull)
      }
    }
  }
}

/**
 * @property name the full name of this package
 * @see McName
 * @since 0.13.0
 * @throws IllegalArgumentException if the [name] parameter is empty or blank
 */
@JvmInline
value class PackageNameImpl internal constructor(override val name: String) : PackageName {
  init {
    require(name.isNotBlank()) {
      "A ${this.javaClass.canonicalName} must be a non-empty, non-blank String.  " +
        "Represent an empty/blank or missing package name as ${DEFAULT::class.qualifiedName}.  " +
        "This name argument, wrapped in single quotes: '$name'"
    }
  }

  override fun append(simpleName: String): String = "$name.$simpleName"

  companion object {
    /**
     * @receiver the String literal representation of a package name
     * @return a [PackageName] from this String literal
     * @since 0.13.0
     */
    fun String.asPackageName(): PackageName = PackageName(this)
  }
}

/**
 * Convenience interface for providing a [PackageName].
 *
 * N.B. Ideally, this interface would be implemented by [DeclaredName], but:
 * 1. By design, [UnqualifiedAndroidResourceDeclaredName] does not have a package name.
 * 2. Even though it's convention, jvm files don't need to have a package name. In this case, it's
 *    tempting to just create a [PackageName] with an empty
 *    string, but it's far more accurate to treat it as null.
 *
 * @since 0.13.0
 */
interface HasPackageName {
  val packageName: PackageName
}
