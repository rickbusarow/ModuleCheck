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

package modulecheck.name

import modulecheck.name.HasSimpleNames.Companion.checkSimpleNames
import modulecheck.name.SimpleName.Companion.asSimpleName
import modulecheck.utils.capitalize
import modulecheck.utils.lazy.unsafeLazy

/** example: `com.example.databinding.FragmentListBinding` */
class AndroidDataBindingName(
  sourceLayout: AndroidResourceName,
  override val packageName: PackageName
) : NameWithPackageName, AndroidName {

  override val simpleNames: List<SimpleName> by unsafeLazy {

    val simpleBindingName = sourceLayout.identifier.asString
      .split("_")
      .joinToString("") { it.capitalize() }
      .plus("Binding")
      .asSimpleName()

    listOf(
      "databinding".asSimpleName(),
      simpleBindingName
    )
  }

  init {
    checkSimpleNames()
  }
}
