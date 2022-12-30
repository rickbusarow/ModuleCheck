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

import modulecheck.parsing.source.HasSimpleNames.Companion.checkSimpleNames
import modulecheck.parsing.source.McName.CompatibleLanguage
import modulecheck.parsing.source.McName.CompatibleLanguage.JAVA
import modulecheck.parsing.source.McName.CompatibleLanguage.KOTLIN
import modulecheck.parsing.source.McName.CompatibleLanguage.XML
import modulecheck.parsing.source.ReferenceName.Companion.asReferenceName
import modulecheck.parsing.source.SimpleName.Companion.asString
import modulecheck.parsing.source.SimpleName.Companion.stripPackageNameFromFqName
import modulecheck.utils.lazy.unsafeLazy
import modulecheck.utils.singletonList
import org.jetbrains.kotlin.name.FqName

/**
 * Represents a "declaration" -- a named object which can be referenced elsewhere.
 *
 * @since 0.12.0
 */
sealed interface DeclaredName : McName, HasSimpleNames {

  /**
   * The languages with which this declaration is compatible. For instance, a member property will
   * typically have a [KOTLIN] declaration using property access syntax, but will also have a
   * [JAVA]/[XML] declaration for setter and getter functions.
   *
   * @since 0.12.0
   */
  val languages: Set<CompatibleLanguage> get() = setOf(KOTLIN, JAVA, XML)

  companion object {

    /**
     * Shorthand for creating a [QualifiedDeclaredName] which is only accessible from Kotlin files.
     *
     * @see McName.CompatibleLanguage.KOTLIN
     * @since 0.12.0
     */
    fun kotlin(
      packageName: PackageName,
      simpleNames: Iterable<SimpleName>
    ): QualifiedDeclaredName = QualifiedDeclaredNameImpl(
      packageName = packageName,
      simpleNames = simpleNames.toList(),
      languages = setOf(KOTLIN)
    )

    /**
     * Shorthand for creating a [QualifiedDeclaredName] which is only accessible from Java or XML
     * files.
     *
     * @see McName.CompatibleLanguage.JAVA
     * @see McName.CompatibleLanguage.XML
     * @since 0.12.0
     */
    fun java(
      packageName: PackageName,
      simpleNames: Iterable<SimpleName>
    ): QualifiedDeclaredName = QualifiedDeclaredNameImpl(
      packageName = packageName,
      simpleNames = simpleNames.toList(),
      languages = setOf(JAVA, XML)
    )

    /**
     * Shorthand for creating a [QualifiedDeclaredName] which is accessible from files in any
     * language.
     *
     * @see McName.CompatibleLanguage.JAVA
     * @see McName.CompatibleLanguage.KOTLIN
     * @see McName.CompatibleLanguage.XML
     * @since 0.12.0
     */
    fun agnostic(
      packageName: PackageName,
      simpleNames: Iterable<SimpleName>
    ): QualifiedDeclaredName = QualifiedDeclaredNameImpl(
      packageName = packageName,
      simpleNames = simpleNames.toList(),
      languages = setOf(KOTLIN, JAVA, XML)
    )
  }
}

/**
 * Represents a "declaration" -- a named object which can be referenced elsewhere.
 *
 * @since 0.12.0
 */
sealed class QualifiedDeclaredName :
  DeclaredName,
  McName,
  HasPackageName,
  HasSimpleNames,
  ResolvableMcName {

  override val name: String
    get() = packageName.append(simpleNames.asString())

  override val segments: List<String> by unsafeLazy { name.split('.') }

  open fun asReferenceName(language: CompatibleLanguage): ReferenceName {
    return name.asReferenceName(language)
  }

  /**
   * `true` if a declaration is top-level in a file, otherwise `false` such as if the declaration is
   * a nested type or a member declaration
   *
   * @since 0.13.0
   */
  val isTopLevel: Boolean by unsafeLazy { simpleNames.size == 1 }

  final override fun equals(other: Any?): Boolean {
    if (this === other) return true

    when (other) {
      is ReferenceName -> {

        if (name != other.name) return false
        if (!languages.contains(other.language)) return false
      }

      is QualifiedDeclaredName -> {

        if (name != other.name) return false
        if (languages != other.languages) return false
      }

      else -> return false
    }
    return true
  }

  final override fun hashCode(): Int = name.hashCode()

  final override fun toString(): String =
    "(${this::class.java.simpleName}) `$name`  language=$languages"
}

internal class QualifiedDeclaredNameImpl(
  override val packageName: PackageName,
  override val simpleNames: List<SimpleName>,
  override val languages: Set<CompatibleLanguage>
) : QualifiedDeclaredName() {
  init {
    checkSimpleNames()
  }
}

/**
 * @return a [QualifiedDeclaredName], where the String after [packageName] is split and treated as
 *   the collection of [SimpleNames][SimpleName].
 * @since 0.12.0
 */
fun FqName.asDeclaredName(
  packageName: PackageName,
  vararg languages: CompatibleLanguage
): QualifiedDeclaredName {
  return asString().stripPackageNameFromFqName(packageName).asDeclaredName(packageName, *languages)
}

/**
 * @return a [QualifiedDeclaredName] from the [packageName] argument, appending the receiver
 *   [SimpleNames][SimpleName]
 * @since 0.12.0
 */
fun Iterable<SimpleName>.asDeclaredName(
  packageName: PackageName,
  vararg languages: CompatibleLanguage
): QualifiedDeclaredName {
  return when {
    languages.isEmpty() -> DeclaredName.agnostic(packageName, this)
    !languages.contains(JAVA) -> DeclaredName.kotlin(packageName, this)
    !languages.contains(KOTLIN) -> DeclaredName.java(packageName, this)
    else -> DeclaredName.agnostic(packageName, this)
  }
}

/**
 * @return a [QualifiedDeclaredName] from the [packageName] argument, appending the receiver
 *   [SimpleNames][SimpleName]
 * @since 0.13.0
 */
fun SimpleName.asDeclaredName(
  packageName: PackageName,
  vararg languages: CompatibleLanguage
): QualifiedDeclaredName {
  return singletonList().asDeclaredName(packageName, *languages)
}
