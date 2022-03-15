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
import modulecheck.utils.LazySet
import org.jetbrains.kotlin.name.FqName

@JvmInline
value class DeclarationName(val fqName: String) {
  override fun toString(): String = "(DeclarationName) `$fqName`"
}

fun String.asDeclarationName(): DeclarationName = DeclarationName(this)

fun FqName.asDeclarationName(): DeclarationName = DeclarationName(asString())

operator fun Set<DeclarationName>.contains(reference: Reference): Boolean {
  return when (reference) {
    is InterpretedReference -> reference.possibleNames.any { it in this }
    is ExplicitReference -> reference.fqName.asDeclarationName() in this
    is UnqualifiedRReference -> reference.fqName.asDeclarationName() in this
  }
}

suspend fun LazySet<DeclarationName>.contains(reference: Reference): Boolean {
  return when (reference) {
    is InterpretedReference -> reference.possibleNames.any { contains(it) }
    is ExplicitReference -> contains(reference.fqName.asDeclarationName())
    is UnqualifiedRReference -> contains(reference.fqName.asDeclarationName())
  }
}

@JvmName("containsDeclarationName")
operator fun Set<DeclarationName>.contains(nameAsString: String): Boolean {
  return nameAsString.asDeclarationName() in this
}

suspend fun LazySet<DeclarationName>.contains(nameAsString: String): Boolean {
  return contains(nameAsString.asDeclarationName())
}
