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

import modulecheck.parsing.source.Reference.JavaReference
import modulecheck.parsing.source.Reference.KotlinReference
import modulecheck.utils.safeAs
import org.jetbrains.kotlin.name.FqName

/** Represents a "declaration" -- a named object which can be referenced elsewhere. */
sealed interface DeclaredName : NamedSymbol {

  companion object {
    operator fun invoke(fqName: String) = AgnosticDeclaredName(fqName)
  }
}

sealed interface JavaCompatibleDeclaredName : DeclaredName
sealed interface KotlinCompatibleDeclaredName : DeclaredName
sealed interface XmlCompatibleDeclaredName : JavaCompatibleDeclaredName, DeclaredName

/** example: `com.example.app.R` */
class AndroidRDeclaredName(override val name: String) :
  DeclaredName,
  KotlinCompatibleDeclaredName,
  JavaCompatibleDeclaredName,
  XmlCompatibleDeclaredName {
  init {
    check(name.endsWith(".R")) {
      "An ${this::class.java.simpleName} fqName must be the base package, appended with `.R`, " +
        "such as `com.example.app.R`.  This instance's fqName is: `$name`."
    }
  }

  override fun equals(other: Any?): Boolean {
    return matches(
      other,
      ifReference = { name == it.name },
      ifDeclaration = { name == it.safeAs<AndroidRDeclaredName>()?.name }
    )
  }

  override fun hashCode(): Int = name.hashCode()

  override fun toString(): String = "(${this::class.java.simpleName}) `$name`"
}

/**
 * Represents names which can only be referenced from Kotlin.
 *
 * For instance, given this top-level property:
 *
 * ```
 * // File.kt
 * package com.example
 *
 * val someProperty = true
 * ```
 *
 * Kotlin code will access this as `com.example.someProperty`.
 *
 * In Java, this will be accessed as `com.example.FileKt.getSomeProperty();`
 *
 * These Kotlin-specific declarations will only match to the [KotlinReference] type.
 *
 * @see KotlinReference
 * @see JavaSpecificDeclaredName
 */
class KotlinSpecificDeclaredName(override val name: String) :
  DeclaredName,
  KotlinCompatibleDeclaredName {

  override fun equals(other: Any?): Boolean {
    return matches(
      other,
      ifReference = { name == it.safeAs<KotlinReference>()?.name },
      ifDeclaration = { name == it.safeAs<KotlinSpecificDeclaredName>()?.name }
    )
  }

  override fun hashCode(): Int = name.hashCode()

  override fun toString(): String = "(${this::class.java.simpleName}) `$name`"
}

/**
 * Represents names which can only be referenced from Java.
 *
 * For instance, given this top-level property:
 *
 * ```
 * // File.kt
 * package com.example
 *
 * val someProperty = true
 * ```
 *
 * Kotlin code will access this as `com.example.someProperty`.
 *
 * In Java, this will be accessed as `com.example.FileKt.getSomeProperty();`
 *
 * These Java-specific declarations will only match to the [JavaReference] type.
 *
 * @see JavaReference
 * @see JavaSpecificDeclaredName
 */
class JavaSpecificDeclaredName(override val name: String) :
  DeclaredName,
  JavaCompatibleDeclaredName,
  XmlCompatibleDeclaredName {

  override fun equals(other: Any?): Boolean {
    return matches(
      other,
      ifReference = { name == it.safeAs<JavaReference>()?.name },
      ifDeclaration = { name == it.safeAs<JavaSpecificDeclaredName>()?.name }
    )
  }

  override fun hashCode(): Int = name.hashCode()

  override fun toString(): String = "(${this::class.java.simpleName}) `$name`"
}

/**
 * Represents names which can only be referenced from either Java or Kotlin.
 *
 * These language-neutral declarations will match to any [JavaReference] or [KotlinReference] type.
 *
 * @see JavaReference
 * @see JavaSpecificDeclaredName
 */
class AgnosticDeclaredName(override val name: String) :
  DeclaredName,
  JavaCompatibleDeclaredName,
  KotlinCompatibleDeclaredName,
  XmlCompatibleDeclaredName {

  override fun equals(other: Any?): Boolean {
    return matches(
      other,
      ifReference = { name == it.name },
      ifDeclaration = { name == it.safeAs<AgnosticDeclaredName>()?.name }
    )
  }

  override fun hashCode(): Int = name.hashCode()

  override fun toString(): String = "(${this::class.java.simpleName}) `$name`"
}

fun String.asAndroidRDeclaration(): AndroidRDeclaredName = AndroidRDeclaredName(this)

fun String.asKotlinDeclarationName(): DeclaredName = KotlinSpecificDeclaredName(this)

fun String.asJavaDeclarationName(): DeclaredName = JavaSpecificDeclaredName(this)

fun String.asDeclarationName(): DeclaredName = AgnosticDeclaredName(this)

fun FqName.asDeclarationName(): DeclaredName = AgnosticDeclaredName(asString())

@JvmName("containsDeclarationName")
fun Set<DeclaredName>.containsAny(names: Set<String>): Boolean {
  return names.any { it.asDeclarationName() in this }
}
