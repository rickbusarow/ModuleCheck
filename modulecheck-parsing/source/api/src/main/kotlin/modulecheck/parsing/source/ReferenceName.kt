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

import modulecheck.parsing.source.McName.CompatibleLanguage
import modulecheck.parsing.source.McName.CompatibleLanguage.JAVA
import modulecheck.parsing.source.McName.CompatibleLanguage.KOTLIN
import modulecheck.parsing.source.McName.CompatibleLanguage.XML
import modulecheck.utils.lazy.LazySet
import modulecheck.utils.lazy.unsafeLazy

/** Marker which indicates that [references] exist. Typically implemented by "file" types */
interface HasReferences {

  /** The references in this object, calculated lazily */
  val references: LazySet<ReferenceName>
}

/** Represents a name -- fully qualified or not -- which references a declaration somewhere else */
sealed class ReferenceName : McName {
  /** The [language][CompatibleLanguage] of the file making this reference */
  abstract val language: CompatibleLanguage

  override val segments: List<String> by unsafeLazy { name.split('.') }

  /** This reference is from a Java source file */
  fun isJava() = language == JAVA

  /** This reference is from a Kotlin source file */
  fun isKotlin() = language == KOTLIN

  /** This reference is from an xml source file */
  @Suppress("GrazieInspection")
  fun isXml() = language == XML

  override fun equals(other: Any?): Boolean {
    if (this === other) return true

    when (other) {
      is ReferenceName -> {

        if (name != other.name) return false
        if (language != other.language) return false
      }

      is QualifiedDeclaredName -> {

        if (name != other.name) return false
        if (!other.languages.contains(language)) return false
      }

      else -> return false
    }
    return true
  }

  final override fun hashCode(): Int = name.hashCode()

  override fun toString(): String {
    return "${this::class.simpleName!!}(name='$name'  language=$language)"
  }

  companion object {
    /** @return a basic [ReferenceName] for this name and language. */
    operator fun invoke(
      name: String,
      language: CompatibleLanguage
    ): ReferenceName = ReferenceNameImpl(
      name = name, language = language
    )

    /** @return a basic [ReferenceName] for this name and language. */
    fun String.asReferenceName(
      language: CompatibleLanguage
    ) = ReferenceName(this, language)
  }
}

private class ReferenceNameImpl(
  override val name: String,
  override val language: CompatibleLanguage
) : ReferenceName(), McName {
  override val simpleName by unsafeLazy {
    name.split('.').last()
  }
}
