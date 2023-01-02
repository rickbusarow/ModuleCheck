/*
 * Copyright (C) 2021-2023 Rick Busarow
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

import kotlinx.serialization.Serializable
import modulecheck.parsing.source.McName.CompatibleLanguage
import modulecheck.parsing.source.SimpleName.Companion.asSimpleName
import modulecheck.utils.lazy.unsafeLazy

/**
 * Any reference to an Android resource
 *
 * @since 0.12.0
 */
@Serializable
sealed class AndroidResourceReferenceName() : ReferenceName()

/**
 * example: `com.example.R`
 *
 * @property packageName the package of this reference (which is just the full string, minus `.R`)
 * @property language the language making this reference
 * @since 0.12.0
 */
@Serializable
class AndroidRReferenceName(
  val packageName: PackageName,
  override val language: CompatibleLanguage
) : AndroidResourceReferenceName() {
  override val name = packageName.append("R")
}

/**
 * example: `R.string.app_name`
 *
 * @param name `R.string.____`
 * @property language the language making this reference
 * @since 0.12.0
 */
// hashcode behavior is intentionally handled by super
@Suppress("EqualsWithHashCodeExist", "EqualsOrHashCode")
@Serializable
class UnqualifiedAndroidResourceReferenceName(
  override val name: String,
  override val language: CompatibleLanguage
) : AndroidResourceReferenceName(),
  HasSimpleNames {

  private val split by unsafeLazy {
    name.split('.').also {
      @Suppress("MagicNumber")
      require(it.size == 3) {
        "The name `$name` must follow the format `R.<prefix>.<identifier>`, " +
          "such as `R.string.app_name`."
      }
    }
  }

  /**
   * example: 'string' in `R.string.app_name`
   *
   * @since 0.12.0
   */
  val prefix: SimpleName by unsafeLazy { split[1].asSimpleName() }

  /**
   * example: 'app_name' in `R.string.app_name`
   *
   * @since 0.12.0
   */
  val identifier: SimpleName by unsafeLazy { split[2].asSimpleName() }

  override val simpleNames: List<SimpleName> by unsafeLazy {
    listOf("R".asSimpleName(), prefix, identifier)
  }

  override fun equals(other: Any?): Boolean {
    if (other is UnqualifiedAndroidResource) {
      return name == other.name
    }

    return super.equals(other)
  }
}

/**
 * example: `com.example.databinding.FragmentViewBinding`
 *
 * @param name `com.example.databinding.FragmentViewBinding`
 * @property language the language making this reference
 * @since 0.12.0
 */
@Serializable
class AndroidDataBindingReferenceName(
  override val name: String,
  override val language: CompatibleLanguage
) : AndroidResourceReferenceName()

/**
 * example: `com.example.R.string.app_name`
 *
 * @param name `com.example.R.string.app_name`
 * @property language the language making this reference
 * @since 0.12.0
 */
@Serializable
class QualifiedAndroidResourceReferenceName(
  override val name: String,
  override val language: CompatibleLanguage
) : AndroidResourceReferenceName()
