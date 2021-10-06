/*
 * Copyright (C) 2021 Rick Busarow
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

package modulecheck.api

import modulecheck.parsing.psi.DeclarationName

abstract class JvmFile {
  abstract val name: String
  abstract val packageFqName: String
  abstract val imports: Set<String>
  abstract val declarations: Set<DeclarationName>

  override fun toString(): String {
    return """${this::class.simpleName}(
      |packageFqName='$packageFqName',
      |
      |importDirectives=$imports,
      |
      |declarations=$declarations
      |
      |)""".trimMargin()
  }

  abstract val wildcardImports: Set<String>
  abstract val maybeExtraReferences: Set<String>
}
