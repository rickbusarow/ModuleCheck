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

package modulecheck.parsing.element.kotlin

import modulecheck.parsing.source.ReferenceName
import modulecheck.parsing.source.ReferenceName.ExplicitKotlinReferenceName
import modulecheck.parsing.source.asExplicitKotlinReference
import modulecheck.utils.lazy.unsafeLazy
import modulecheck.utils.requireNotNull
import org.jetbrains.kotlin.psi.KtImportDirective

internal class ImportParser(private val importDirectives: List<KtImportDirective>) {

  private val _aliasMap = mutableMapOf<String, ReferenceName.ExplicitKotlinReferenceName>()

  val aliasMap: Map<String, ExplicitKotlinReferenceName> by unsafeLazy {
    imports
    _aliasMap
  }

  val imports: Set<ExplicitKotlinReferenceName> by lazy {

    importDirectives
      .asSequence()
      .filter { it.isValidImport }
      .filter { !it.isAllUnder }
      .filter { it.importPath != null }
      .map { directive ->
        directive.importPath.requireNotNull()
          .pathStr
          .asExplicitKotlinReference()
          .also { it.maybeCacheAlias(directive) }
      }
      .toSet()
  }

  val wildcards: Set<String> by lazy {

    importDirectives
      .asSequence()
      .filter { it.isValidImport }
      .filter { it.isAllUnder }
      .filter { it.importPath != null }
      .map { directive ->
        directive.importPath.requireNotNull()
          .pathStr
      }
      .toSet()
  }

  private fun ExplicitKotlinReferenceName.maybeCacheAlias(
    importDirective: KtImportDirective
  ) {
    val explicitReference = this

    // Map an alias to its actual name, so that it can be looked up/inlined while resolving
    importDirective.alias
      // The KtImportAlias is `as Foo`.  It has three children:
      // [LeafPsiElement, PsiWhiteSpace, LeafPsiElement], which are [`as`, ` `, `Foo`]
      // respectively.
      ?.lastChild
      ?.text
      ?.let { alias ->
        _aliasMap[alias] = explicitReference
      }
  }
}
