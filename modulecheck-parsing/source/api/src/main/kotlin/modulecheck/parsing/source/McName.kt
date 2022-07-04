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

/**
 * Fundamentally, this is a version of `FqName` (such as Kotlin's
 * [FqName][org.jetbrains.kotlin.name.FqName]) with syntactic sugar for complex matching
 * requirements.
 *
 * @see QualifiedDeclaredName
 * @see ReferenceName
 */
sealed interface McName : Comparable<McName> {
  /** The raw String value of this name, such as `com.example.lib1.Lib1Class`. */
  val name: String

  /** ex: 'com.example.Subject' has the segments ['com', 'example', 'Subject'] */
  val segments: List<String>

  /**
   * The simplest name. For an inner class like `com.example.Outer.Inner`, this will be 'Inner'.
   */
  val simpleName: String
    get() = segments.last()

  /** @return true if this [name] value with the name string of [other], otherwise false */
  fun startsWith(other: McName): Boolean {
    return name.startsWith(other.name)
  }

  /** @return true if this [name] value ends with the [str] parameter, otherwise false */
  fun endsWith(str: String): Boolean {
    return name.endsWith(str)
  }

  /** @return true if the last segment of this symbol matches [str], otherwise false */
  fun endsWithSimpleName(str: String): Boolean {
    return name.split('.').last() == str
  }

  fun endsWith(symbol: McName): Boolean {
    return name.endsWith(symbol.name)
  }

  override fun compareTo(other: McName): Int {
    // sort by name first, then by type.
    return compareValuesBy(
      this,
      other,
      { it.name },
      { it::class.java.simpleName }
    )
  }

  /**
   * The language which contains a given [ReferenceName], or the language which can access a given
   * [DeclaredName]
   */
  sealed interface CompatibleLanguage {
    /** Java */
    object JAVA : CompatibleLanguage {
      override fun toString(): String = this::class.java.simpleName
    }

    /** Kotlin */
    object KOTLIN : CompatibleLanguage {
      override fun toString(): String = this::class.java.simpleName
    }

    /** Xml, which is treated the same as [JAVA] */
    object XML : CompatibleLanguage {
      override fun toString(): String = this::class.java.simpleName
    }
  }
}
