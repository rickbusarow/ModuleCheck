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

import modulecheck.parsing.source.Reference.ExplicitReference
import modulecheck.parsing.source.Reference.InterpretedReference
import modulecheck.parsing.source.Reference.UnqualifiedRReference
import modulecheck.utils.LazySet.DataSource
import modulecheck.utils.mapToSet
import org.jetbrains.kotlin.name.FqName

sealed interface Reference {

  @JvmInline
  value class ExplicitReference(val fqName: String) : Reference

  @JvmInline
  value class UnqualifiedRReference(val fqName: String) : Reference

  @JvmInline
  value class InterpretedReference(val possibleNames: Set<String>) : Reference

  fun logString(): String = when (this) {
    is ExplicitReference -> fqName
    is InterpretedReference -> possibleNames.joinToString()
    is UnqualifiedRReference -> fqName
  }

  fun startsWith(str: String): Boolean {
    return when (this) {
      is ExplicitReference -> fqName.startsWith(str)
      is InterpretedReference ->
        possibleNames
          .any { it.startsWith(str) }
      is UnqualifiedRReference -> fqName.startsWith(str)
    }
  }

  fun startingWith(str: String): List<String> {
    return when (this) {
      is ExplicitReference -> if (fqName.startsWith(str)) {
        listOf(fqName)
      } else {
        listOf()
      }
      is InterpretedReference ->
        possibleNames
          .filter { it.startsWith(str) }
      is UnqualifiedRReference -> if (fqName.startsWith(str)) {
        listOf(fqName)
      } else {
        listOf()
      }
    }
  }

  fun endsWith(str: String): Boolean {
    return when (this) {
      is ExplicitReference -> fqName.endsWith(str)
      is InterpretedReference ->
        possibleNames
          .any { it.endsWith(str) }
      is UnqualifiedRReference -> fqName.endsWith(str)
    }
  }

  fun endingWith(str: String): List<String> {
    return when (this) {
      is ExplicitReference -> if (fqName.endsWith(str)) {
        listOf(fqName)
      } else {
        listOf()
      }
      is InterpretedReference ->
        possibleNames
          .filter { it.endsWith(str) }
      is UnqualifiedRReference -> if (fqName.endsWith(str)) {
        listOf(fqName)
      } else {
        listOf()
      }
    }
  }
}

fun String.asExplicitReference(): ExplicitReference = ExplicitReference(this)
fun String.asInterpretedReference(): InterpretedReference = InterpretedReference(setOf(this))
fun FqName.asExplicitReference(): ExplicitReference = ExplicitReference(asString())

operator fun Set<Reference>.contains(declarationName: DeclarationName): Boolean {
  val simple = this.contains(declarationName.fqName.asExplicitReference())

  if (simple) return true

  filterIsInstance<InterpretedReference>()
    .any { it.possibleNames.contains(declarationName.fqName) }
    .let { if (it) return true }

  val rRefs = filterIsInstance<UnqualifiedRReference>()
    .mapToSet { it.fqName }

  if (rRefs.isEmpty()) return false

  val declarationAsR = "\\bR\\..*\$".toRegex()
    .find(declarationName.fqName)
    ?.value
    ?: return false

  return rRefs.contains(declarationAsR)
}

@JvmName("containsReferenceName")
operator fun Set<Reference>.contains(nameAsString: String): Boolean {
  return nameAsString.asExplicitReference() in this
}

fun interface HasReferences {

  fun references(): List<DataSource<Reference>>
}
