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
 * @see DeclaredName
 * @see Reference
 */
sealed interface NamedSymbol : Comparable<NamedSymbol> {
  val name: String

  fun startsWith(symbol: NamedSymbol): Boolean {
    return name.startsWith(symbol.name)
  }

  fun endsWith(str: String): Boolean {
    return name.endsWith(str)
  }

  fun endsWith(symbol: NamedSymbol): Boolean {
    return name.endsWith(symbol.name)
  }

  override fun compareTo(other: NamedSymbol): Int {
    // sort by name first, then by type.
    return compareValuesBy(
      this,
      other,
      { it.name },
      { it::class.java.simpleName }
    )
  }
}

internal inline fun NamedSymbol.matches(
  other: Any?,
  ifReference: (Reference) -> Boolean,
  ifDeclaration: (DeclaredName) -> Boolean
): Boolean {
  if (this === other) return true

  return when (other) {
    is Reference -> ifReference(other)
    is DeclaredName -> ifDeclaration(other)
    else -> false
  }
}
