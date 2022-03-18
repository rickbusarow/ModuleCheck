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

import modulecheck.parsing.source.Reference.UnqualifiedAndroidResourceReference
import org.jetbrains.kotlin.name.FqName

sealed class DeclarationName : NamedSymbol {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true

    if (other is String) return fqName == other

    if (other !is NamedSymbol) return false

    return when (other) {
      is UnqualifiedAndroidResourceReference -> {
        val directMatch = fqName == other.fqName
        if (directMatch) return true

        fqName == "\\bR\\..*\$".toRegex()
          .find(other.fqName)
          ?.value
      }
      else -> fqName == other.fqName
    }
  }

  fun asJavaFriendly() = this as? JavaFriendly
  fun asKotlinFriendly() = this as? KotlinFriendly

  override fun hashCode(): Int {
    return fqName.hashCode()
  }

  override fun toString(): String {
    return "DeclarationName(fqName='$fqName')"
  }

  companion object {
    operator fun invoke(fqName: String) = AgnosticDeclarationName(fqName)
  }
}

interface JavaFriendly {
  val fqName: String
}

interface KotlinFriendly {
  val fqName: String
}

class KotlinSpecificDeclaration(override val fqName: String) : DeclarationName(), KotlinFriendly

class JavaSpecificDeclaration(override val fqName: String) : DeclarationName(), JavaFriendly

class AgnosticDeclarationName(override val fqName: String) :
  DeclarationName(),
  KotlinFriendly,
  JavaFriendly

fun String.asDeclarationName(): DeclarationName = AgnosticDeclarationName(this)
fun String.asKotlinDeclarationName(): DeclarationName = KotlinSpecificDeclaration(this)
fun String.asJavaDeclarationName(): DeclarationName = JavaSpecificDeclaration(this)

fun FqName.asDeclarationName(): DeclarationName = AgnosticDeclarationName(asString())

@JvmName("containsDeclarationName")
fun Set<DeclarationName>.containsAny(names: Set<String>): Boolean {
  return names.any { it.asDeclarationName() in this }
}
