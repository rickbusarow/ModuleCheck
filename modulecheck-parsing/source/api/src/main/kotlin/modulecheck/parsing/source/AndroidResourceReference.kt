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

import modulecheck.parsing.source.Reference.AgnosticReference
import modulecheck.parsing.source.Reference.ExplicitReference
import modulecheck.parsing.source.Reference.JavaReference
import modulecheck.parsing.source.Reference.KotlinReference
import modulecheck.utils.lazy.unsafeLazy
import modulecheck.utils.safeAs

sealed interface AndroidResourceReference : Reference, AgnosticReference

class AndroidRReference(override val name: String) :
  AndroidResourceReference,
  ExplicitReference,
  KotlinReference,
  JavaReference {

  override fun equals(other: Any?): Boolean {
    return matches(
      other = other,
      ifReference = {
        name == (it.safeAs<AndroidRReference>()?.name ?: it.safeAs<ExplicitReference>()?.name)
      },
      ifDeclaration = { name == it.safeAs<AndroidRDeclaredName>()?.name }
    )
  }

  override fun hashCode(): Int = name.hashCode()

  override fun toString(): String = "(${this::class.java.simpleName}) `$name`"
}

class UnqualifiedAndroidResourceReference(override val name: String) :
  AndroidResourceReference,
  ExplicitReference {

  private val split by unsafeLazy {
    name.split('.').also {
      @Suppress("MagicNumber")
      require(it.size == 3) {
        "The name `$name` must follow the format `R.<prefix>.<identifier>`, " +
          "such as `R.string.app_name`."
      }
    }
  }

  val prefix by unsafeLazy { split[1] }
  val identifier by unsafeLazy { split[2] }

  override fun equals(other: Any?): Boolean {
    return matches(
      other = other,
      ifReference = { name == it.safeAs<UnqualifiedAndroidResourceReference>()?.name },
      ifDeclaration = { name == it.safeAs<AndroidResourceDeclaredName>()?.name }
    )
  }

  override fun hashCode(): Int = name.hashCode()

  override fun toString(): String = "(${this::class.java.simpleName}) `$name`"
}

class AndroidDataBindingReference(override val name: String) :
  AndroidResourceReference,
  ExplicitReference {

  override fun equals(other: Any?): Boolean {
    return matches(
      other = other,
      ifReference = { name == it.name },
      ifDeclaration = { name == it.safeAs<AndroidResourceDeclaredName>()?.name }
    )
  }

  override fun hashCode(): Int = name.hashCode()

  override fun toString(): String = "(${this::class.java.simpleName}) `$name`"
}

class QualifiedAndroidResourceReference(override val name: String) :
  AndroidResourceReference,
  ExplicitReference,
  KotlinReference,
  JavaReference {

  override fun equals(other: Any?): Boolean {
    return matches(
      other = other,
      ifReference = {
        name == (it.safeAs<AndroidRReference>()?.name ?: it.safeAs<ExplicitReference>()?.name)
      },
      ifDeclaration = { name == it.safeAs<GeneratedAndroidResourceDeclaredName>()?.name }
    )
  }

  override fun hashCode(): Int = name.hashCode()

  override fun toString(): String = "(${this::class.java.simpleName}) `$name`"
}
